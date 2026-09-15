# ds-storage: ServiceConfig migrated to SmallRye Config (PoC)

This is a proof of concept for replacing the kb-util `YAML`-backed `ServiceConfig` with
[SmallRye Config](https://smallrye.io/smallrye-config/) (a standalone MicroProfile Config
implementation — this project does not use Quarkus). Scope: `ds-storage` only, plus one small,
necessary touch in `ds-shared` (see below).

## What changed

| File | Change |
|---|---|
| `ds-storage/pom.xml` | Added `smallrye-config` and `smallrye-config-source-yaml` (3.12.4). |
| `ds-storage/src/main/java/dk/kb/storage/config/ServiceConfig.java` | Rewritten on top of `SmallRyeConfig`. Public API (`initialize`, `getConfig`, `getDBDriver`, `getDBUrl`, `getDBUserName`, `getDBPassword`, `getConnectionPoolSize`, `getDBBatchSize`, `getAllowedOrigins`) is unchanged, plus new `setRuntimeProperty` / `clearRuntimeProperty` / `getRuntimePropertyNames`. |
| `ds-storage/src/main/java/dk/kb/storage/webservice/KBOAuth2Handler.java` | Switched from `YAML.getSubMap("security")` to reading `security.*` keys directly from the MicroProfile `Config`. Behaviour unchanged, including the warning when no `security` section is configured at all. |
| `ds-storage/conf/ds-storage-behaviour.yaml` | One line changed: `${env:TMPDIR:-/tmp}` → `${TMPDIR:/tmp}` (see "YAML stays almost the same" below). |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigRuntimeInjectionTest.java` | **New.** Demonstrates runtime property injection. |
| `ds-shared/pom.xml` | Added `microprofile-config-api` (interface only, no implementation — see below). |
| `ds-shared/src/main/java/dk/kb/util/webservice/OpenApiResource.java` | `setConfig(YAML)` → `setConfig(Config)`. Still supports `${config:yaml.path}` and `${config:origins[*].name}` (wildcard) placeholders in OpenAPI specs, now resolved against MicroProfile Config instead of YAML. |

Nothing else needed to change: `ContextListener.initialize(configFile)`, `DsStorage`,
`DsStorageApiServiceImpl`, `DsStorageFacade` and `Application_v1` all call `ServiceConfig`
exactly as before and did not need edits.

### Why touch ds-shared?

`Application_v1.getClasses()` calls `OpenApiResource.setConfig(ServiceConfig.getConfig())` to let
`OpenApiResource` (in `ds-shared`) substitute `${config:...}` placeholders in the OpenAPI spec
(e.g. `${config:openapi.serverurl}`, and the wildcard `${config:origins[*].name}` used to build the
enum of allowed origins). That method's signature is defined in `ds-shared` and previously required a
kb-util `YAML`. It was changed to accept the standard `org.eclipse.microprofile.config.Config`
interface instead — `ds-shared` now only depends on the MicroProfile Config *API* (no concrete
implementation), so it stays decoupled from the choice of SmallRye Config made in `ds-storage`.

## YAML stays almost the same

`ds-storage-behaviour.yaml`, `ds-storage-environment.yaml` and `ds-storage-local.yaml` keep their
structure, keys and the three-layer behaviour/environment/local pattern described in `DEVELOPER.md`.
They are still located and layered via the exact same mechanism as before: the `application-config`
JNDI/context value (a glob such as `/app/conf/ds-storage*.yaml`) is resolved with kb-util's
`Resolver.resolveGlob(...)`, and the matching files are loaded in alphanumerical order — so
`ds-storage-behaviour.yaml` < `ds-storage-environment.yaml` < `ds-storage-local.yaml`, with later
files overriding earlier ones key by key. `ContextListener` is unchanged.

One line had to change because of a genuine syntax difference between kb-util's extrapolation
(Apache Commons Text `StringSubstitutor`) and SmallRye Config's own `${...}` expression syntax:

```diff
- url: jdbc:h2:${env:TMPDIR:-/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
+ url: jdbc:h2:${TMPDIR:/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

Environment variables are already a config source in SmallRye Config (ordinal 300), so `${TMPDIR}`
resolves directly against the `TMPDIR` env var; `${TMPDIR:/tmp}` adds the same `/tmp` fallback the
old `:-` syntax provided. No other expression in the current YAML files needed changing.

## Known semantic difference: overriding list values

SmallRye Config flattens a YAML list into indexed properties, e.g. `origins[0].name`,
`origins[1].name`, .... Each property is then resolved independently, highest-ordinal source wins,
same as any other key. kb-util's old merge instead replaced the *entire* `origins` list as soon as
an overriding file (environment/local) redefined it at all (`MERGE_ACTION.keep_extra` for lists).

Concretely: if `ds-storage-environment.yaml` defines only 2 origins (as
`ds-storage-environment.yaml.SAMPLE` does) while `ds-storage-behaviour.yaml` defines 14, the old
code ended up with exactly those 2 origins. A naive per-index SmallRye read would instead end up
with 14 origins, where only indices 0 and 1 are overridden by the environment file.

`ServiceConfig.loadAllowedOrigins()` special-cases this: it finds the single highest-ordinal config
source that defines *any* `origins[...]` entry and reads the whole list from that source alone,
which reproduces the old "whole list wins" behaviour for `origins` specifically. This is not a
generic solution for arbitrary YAML lists — if a future config value needs the same treatment, apply
the same pattern (or introduce a `@ConfigMapping` with an explicit `List<T>` and accept SmallRye's
per-index override semantics).

## Runtime property injection

`ServiceConfig` registers a small in-memory `ConfigSource` (`ServiceConfig.RuntimeConfigSource`) at
the highest ordinal (500) of all sources used — above system properties (400), environment variables
(300) and the layered YAML files (100+). SmallRye Config re-reads all sources on every
`getValue`/`getOptionalValue` call rather than caching a snapshot (unlike the old `YAML`, which was
immutable once loaded), so:

```java
ServiceConfig.setRuntimeProperty("db.connectionPoolSize", "42"); // takes effect immediately
ServiceConfig.getConnectionPoolSize();                           // -> 42, no restart needed
ServiceConfig.clearRuntimeProperty("db.connectionPoolSize");     // reverts to the YAML value (10)
```

See `ServiceConfigRuntimeInjectionTest` for a runnable demonstration, including injecting a key that
doesn't exist in any YAML file at all. `setRuntimeProperty` redacts values in its log line for keys
that look like passwords/secrets/tokens.

This PoC deliberately stops at the Java API level (per the scoping decision for this round): there is
no HTTP/admin endpoint yet to call `setRuntimeProperty` from outside the JVM. Adding one (e.g. a
`POST /v1/admin/config` guarded by an admin role) would be a natural next step if runtime injection
needs to be operable from outside the process.

## Suggested follow-ups (out of scope for this PoC)

* Apply the same migration to the other `ds-backend` submodules once this pattern is validated.
* Consider `@ConfigMapping` interfaces for strongly-typed, validated config sections instead of raw
  `getValue(String, Class)` calls, for sections that don't need runtime mutability.
* Decide whether runtime-injected properties should be reachable via an admin API and, if so, what
  authorization/audit trail that needs.
