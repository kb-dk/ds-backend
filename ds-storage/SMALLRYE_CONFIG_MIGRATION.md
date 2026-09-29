# ds-storage: ServiceConfig migrated to SmallRye Config (PoC)

This is a proof of concept for replacing the kb-util `YAML`-backed `ServiceConfig` with
[SmallRye Config](https://smallrye.io/smallrye-config/) (a standalone MicroProfile Config
implementation — this project does not use Quarkus). Scope: `ds-storage` only, plus one small,
necessary touch in `ds-shared` (see below).

## What changed

| File | Change |
|---|---|
| `ds-storage/pom.xml` | Added `smallrye-config` and `smallrye-config-source-yaml` (3.12.4). |
| `ds-storage/src/main/java/dk/kb/storage/config/ServiceConfig.java` | Rewritten on top of `SmallRyeConfig`. `initialize(String... configFiles)` (multi-file glob) became `initialize(String configFile)` (single file, resolved with `Resolver.resolveURL(...)`). The rest of the public API (`getConfig`, `getDBDriver`, `getDBUrl`, `getDBUserName`, `getDBPassword`, `getConnectionPoolSize`, `getDBBatchSize`, `getAllowedOrigins`) is unchanged, plus new `setRuntimeProperty` / `clearRuntimeProperty` / `getRuntimePropertyNames`. |
| `ds-storage/src/main/java/dk/kb/storage/webservice/KBOAuth2Handler.java` | Switched from `YAML.getSubMap("security")` to reading `security.*` keys directly from the MicroProfile `Config`. Behaviour unchanged, including the warning when no `security` section is configured at all. |
| `ds-storage/conf/ds-storage-behaviour.yaml` | One line changed: `${env:TMPDIR:-/tmp}` → `${TMPDIR:/tmp}` (see "YAML stays almost the same" below). |
| `ds-storage/conf/ocp/ds-storage.xml` | The `application-config` Tomcat context entry changed from a glob (`/app/conf/ds-storage*.yaml`) to a single path (`/app/conf/ds-storage.yaml`). |
| `ds-storage/src/test/jetty/jetty-env.xml` | Same change for local Jetty runs: `${basedir}/conf/${project.artifactId}*.yaml` → `${basedir}/conf/${project.artifactId}-behaviour.yaml`. |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigTest.java` | Sample test updated to call `initialize(...)` with the single `ds-storage-behaviour.yaml` path, and to check for a `config/application.properties` local override instead of an `ds-storage-environment.yaml`. |
| `ds-storage/src/test/java/dk/kb/storage/integration/DsStorageClientTest.java` | `@Tag("integration")` test updated for the single-file `initialize(...)` signature; the integration-only `ds-storage-integration-test.yaml` overlay (server-internal, not in git) is no longer merged in through `ServiceConfig` — it's read directly with kb-util's `YAML.resolveLayeredConfigs(...)` instead, since it only carries test-only `integration.*` keys. |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigRuntimeInjectionTest.java` | **New.** Demonstrates runtime property injection. |
| `ds-shared/pom.xml` | Added `microprofile-config-api` (interface only, no implementation — see below). |
| `ds-shared/src/main/java/dk/kb/util/webservice/OpenApiResource.java` | `setConfig(YAML)` → `setConfig(Config)`. Still supports `${config:yaml.path}` and `${config:origins[*].name}` (wildcard) placeholders in OpenAPI specs, now resolved against MicroProfile Config instead of YAML. |

Nothing else needed to change: `ContextListener.initialize(configFile)` already only ever passed a
single `String` (the one JNDI-looked-up context value), so it works unchanged against the new
single-argument `initialize`. `DsStorage`, `DsStorageApiServiceImpl`, `DsStorageFacade` and
`Application_v1` all call `ServiceConfig` exactly as before and did not need edits either.

### Why touch ds-shared?

`Application_v1.getClasses()` calls `OpenApiResource.setConfig(ServiceConfig.getConfig())` to let
`OpenApiResource` (in `ds-shared`) substitute `${config:...}` placeholders in the OpenAPI spec
(e.g. `${config:openapi.serverurl}`, and the wildcard `${config:origins[*].name}` used to build the
enum of allowed origins). That method's signature is defined in `ds-shared` and previously required a
kb-util `YAML`. It was changed to accept the standard `org.eclipse.microprofile.config.Config`
interface instead — `ds-shared` now only depends on the MicroProfile Config *API* (no concrete
implementation), so it stays decoupled from the choice of SmallRye Config made in `ds-storage`.

## Single YAML file, not three

The previous `ServiceConfig` loaded a **glob** of files (`ds-storage-behaviour.yaml`,
`ds-storage-environment.yaml`, `ds-storage-local.yaml`) resolved via kb-util's
`Resolver.resolveGlob(...)` and layered in alphanumerical order, each overriding the previous one
key by key. That convention has been retired as part of this cleanup:
`ServiceConfig.initialize(String configFile)` now takes exactly **one** YAML file, resolved with
`Resolver.resolveURL(...)` (verbatim file path → classpath → user home). In production that single
path is still configured entirely outside the project, via the same `application-config` Tomcat
context environment entry as before (`conf/ocp/ds-storage.xml`) — it's simply a plain path now
instead of a glob.

Environment- or operator-specific overrides that used to live in a second or third YAML file are
now expressed with SmallRye Config's own built-in layering instead, all of which sit above the
single YAML file (ordinal 100) in priority:

* system properties (ordinal 400) — e.g. `-Ddb.connectionPoolSize=42`
* environment variables (300) — e.g. `DB_CONNECTIONPOOLSIZE=42`
* an optional `.env` file (295) next to the working directory
* an optional `config/application.properties` file (260) — a plain `key=value` file, the closest
  drop-in replacement for the old `ds-storage-local.yaml`/`ds-storage-environment.yaml` pattern
* runtime injection via `ServiceConfig.setRuntimeProperty(...)` (500, highest — see below)

`ds-storage-behaviour.yaml` itself keeps its structure and keys unchanged (see below), and
`ds-storage-environment.yaml.SAMPLE` is no longer read by `ServiceConfig` — it's kept only as a
reference for which keys are commonly overridden, until it's replaced by a
`config/application.properties.SAMPLE` in a follow-up.

## YAML stays almost the same

`ds-storage-behaviour.yaml` keeps its structure and keys exactly as before. One line had to change
because of a genuine syntax difference between kb-util's extrapolation (Apache Commons Text
`StringSubstitutor`) and SmallRye Config's own `${...}` expression syntax:

```diff
- url: jdbc:h2:${env:TMPDIR:-/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
+ url: jdbc:h2:${TMPDIR:/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

Environment variables are already a config source in SmallRye Config (ordinal 300), so `${TMPDIR}`
resolves directly against the `TMPDIR` env var; `${TMPDIR:/tmp}` adds the same `/tmp` fallback the
old `:-` syntax provided. No other expression in the file needed changing.

## Known semantic difference: overriding list values

SmallRye Config flattens a YAML list into indexed properties, e.g. `origins[0].name`,
`origins[1].name`, .... Each property is then resolved independently, highest-ordinal source wins,
same as any other key. kb-util's old merge instead replaced the *entire* `origins` list as soon as
an overriding file redefined it at all (`MERGE_ACTION.keep_extra` for lists).

Now that `ServiceConfig` only loads a single YAML file, this rarely comes up in practice — but it
can still matter if `origins` is ever (partially) redefined via a higher-priority source, e.g. a
`config/application.properties` with `origins[0].name=...`, or a runtime injection. A naive
per-index read would then mix entries from both sources instead of one cleanly replacing the other:
if the override only defines `origins[0]` and `origins[1]` while the YAML file defines 14, a naive
read would end up with 14 origins where only indices 0 and 1 come from the override.

`ServiceConfig.loadAllowedOrigins()` special-cases this: it finds the single highest-ordinal config
source that defines *any* `origins[...]` entry and reads the whole list from that source alone, so
one source always wins outright for `origins`, never a per-index blend. This is not a generic
solution for arbitrary YAML lists — if a future config value needs the same treatment, apply the
same pattern (or introduce a `@ConfigMapping` with an explicit `List<T>` and accept SmallRye's
per-index override semantics).

## Runtime property injection

`ServiceConfig` registers a small in-memory `ConfigSource` (`ServiceConfig.RuntimeConfigSource`) at
the highest ordinal (500) of all sources used — above system properties (400), environment variables
(300) and the single YAML file (100). SmallRye Config re-reads all sources on every
`getValue`/`getOptionalValue` call rather than caching a snapshot (unlike the old `YAML`, which was
immutable once loaded), so:

```java
ServiceConfig.setRuntimeProperty("db.connectionPoolSize", "42"); // takes effect immediately
ServiceConfig.getConnectionPoolSize();                           // -> 42, no restart needed
ServiceConfig.clearRuntimeProperty("db.connectionPoolSize");     // reverts to the YAML value (10)
```

See `ServiceConfigRuntimeInjectionTest` for a runnable demonstration, including injecting a key that
doesn't exist in the YAML file at all. `setRuntimeProperty` redacts values in its log line for keys
that look like passwords/secrets/tokens.

This PoC deliberately stops at the Java API level (per the scoping decision for this round): there is
no HTTP/admin endpoint yet to call `setRuntimeProperty` from outside the JVM. Adding one (e.g. a
`POST /v1/admin/config` guarded by an admin role) would be a natural next step if runtime injection
needs to be operable from outside the process.

## Suggested follow-ups (out of scope for this PoC)

* Apply the same migration to the other `ds-backend` submodules once this pattern is validated.
* Replace `ds-storage-environment.yaml.SAMPLE` with a `config/application.properties.SAMPLE` that
  documents the same commonly-overridden keys in the new format.
* Consider `@ConfigMapping` interfaces for strongly-typed, validated config sections instead of raw
  `getValue(String, Class)` calls, for sections that don't need runtime mutability.
* Decide whether runtime-injected properties should be reachable via an admin API and, if so, what
  authorization/audit trail that needs.
