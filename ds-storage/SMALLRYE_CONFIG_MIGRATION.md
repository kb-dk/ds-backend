# ds-storage: ServiceConfig migrated to SmallRye Config (PoC)

This is a proof of concept for replacing the kb-util `YAML`-backed `ServiceConfig` with
[SmallRye Config](https://smallrye.io/smallrye-config/) (a standalone MicroProfile Config
implementation — this project does not use Quarkus). Scope: `ds-storage` only, plus one small,
necessary touch in `ds-shared` (see below).

## What changed

| File | Change |
|---|---|
| `ds-storage/pom.xml` | Added `smallrye-config` and `smallrye-config-source-yaml` (3.12.4). |
| `ds-storage/src/main/java/dk/kb/storage/config/ServiceConfig.java` | Rewritten on top of `SmallRyeConfig`. `initialize(String... configFiles)` (multi-file glob) became `initialize(String configFile)` (single file, resolved with `Resolver.resolveURL(...)`), plus a new `initialize(String configFile, String propertiesOverrideFile)` overload (see "The real devops override file" below). The rest of the public API (`getConfig`, `getDBDriver`, `getDBUrl`, `getDBUserName`, `getDBPassword`, `getConnectionPoolSize`, `getDBBatchSize`, `getAllowedOrigins`) is unchanged, plus new `setRuntimeProperty` / `clearRuntimeProperty` / `getRuntimePropertyNames`. |
| `ds-storage/src/main/java/dk/kb/storage/webservice/ContextListener.java` | Now also looks up an optional `application-properties-config` JNDI entry and passes it as the second argument to `ServiceConfig.initialize(...)`; logs the resolved path (or that none is configured). |
| `ds-storage/src/main/java/dk/kb/storage/webservice/KBOAuth2Handler.java` | Switched from `YAML.getSubMap("security")` to reading `security.*` keys directly from the MicroProfile `Config`. Behaviour unchanged, including the warning when no `security` section is configured at all. |
| `ds-storage/conf/ds-storage-behaviour.yaml` | One line changed: `${env:TMPDIR:-/tmp}` → `${TMPDIR:/tmp}` (see "YAML stays almost the same" below). |
| `ds-storage/conf/ocp/ds-storage.xml` | The `application-config` Tomcat context entry changed from a glob (`/app/conf/ds-storage*.yaml`) to a single path (`${user.home}/services/conf/ds-storage-behaviour.yaml`). A new `application-properties-config` entry was added, pointing at `${user.home}/services/conf/ds-storage-application.properties` (see below). |
| `ds-storage/src/test/jetty/jetty-env.xml` | Same YAML change for local Jetty runs: `${basedir}/conf/${project.artifactId}*.yaml` → `${basedir}/conf/${project.artifactId}-behaviour.yaml`. A new `propertiesConfig` entry was added too, pointing at `${basedir}/conf/${project.artifactId}-application.properties` (absent by default - a local run is expected to log an error for it, see below). |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigTest.java` | Sample test updated to call `initialize(...)` with the single `ds-storage-behaviour.yaml` path, and to check for a `config/application.properties` local override instead of an `ds-storage-environment.yaml`. |
| `ds-storage/src/test/java/dk/kb/storage/integration/DsStorageClientTest.java` | `@Tag("integration")` test updated for the single-file `initialize(...)` signature; the integration-only `ds-storage-integration-test.yaml` overlay (server-internal, not in git) is no longer merged in through `ServiceConfig` — it's read directly with kb-util's `YAML.resolveLayeredConfigs(...)` instead, since it only carries test-only `integration.*` keys. |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigRuntimeInjectionTest.java` | **New.** Demonstrates runtime property injection. |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigApplicationPropertiesOverrideTest.java` | **New.** Demonstrates SmallRye's own implicit `config/application.properties` convention (single-instance-only, see below). |
| `ds-storage/src/test/java/dk/kb/storage/config/ServiceConfigPropertiesOverrideFileTest.java` | **New.** Demonstrates the real, per-service devops/operations override file (the explicit `initialize(String, String)` argument), including that a configured-but-missing file is logged as an error without failing startup. |
| `ds-storage/config/application.properties.SAMPLE` | **New.** Template for the single-instance-only local override file, replacing `ds-storage-environment.yaml.SAMPLE`. |
| `ds-storage/conf/ds-storage-application.properties.SAMPLE` | **New.** Template for the real, per-service devops/operations override file (see below). |
| `.gitignore` (repo root) | Added `**/config/application.properties` and `*-application.properties` — these files carry secrets (e.g. the real db password) and must never be committed. |
| `ds-shared/pom.xml` | Added `microprofile-config-api` (interface only, no implementation — see below). |
| `ds-shared/src/main/java/dk/kb/util/webservice/OpenApiResource.java` | `setConfig(YAML)` → `setConfig(Config)`. Still supports `${config:yaml.path}` and `${config:origins[*].name}` (wildcard) placeholders in OpenAPI specs, now resolved against MicroProfile Config instead of YAML. |

Nothing else needed to change: `DsStorage`, `DsStorageApiServiceImpl`, `DsStorageFacade` and
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
`ds-storage-environment.yaml.SAMPLE` is no longer read by `ServiceConfig` — it has been replaced by
the two override files described next.

## The real devops/operations override file: an explicit per-service path

The production/development deployment topology turns out to matter here: **a Tomcat instance can
host several of the `ds-backend` WARs at once** (the development server runs all 6 services in one
Tomcat), while production gives each service its own Tomcat instance. That rules out relying on
anything keyed off the JVM process itself (a system property, an environment variable, or a file
found via the shared current working directory) to carry a *per-service* secret such as the database
password — all webapps in that shared Tomcat instance would see the exact same value.

The fix mirrors how the YAML file itself already avoids this problem: `application-config` is a
per-webapp Tomcat context `<Environment>` entry, so `ds-storage` and `ds-present` can each point at
their own YAML file even sharing one Tomcat instance. The properties override file now gets the
same treatment, via a second, optional context entry, `application-properties-config`, and a second,
optional argument to `ServiceConfig.initialize(...)`:

```java
public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException
```

`ContextListener` looks up both JNDI entries and passes them straight through. In
`conf/ocp/ds-storage.xml`:

```xml
<Environment name="application-config"
    value="${user.home}/services/conf/ds-storage-behaviour.yaml"
    type="java.lang.String" override="false"/>
<Environment name="application-properties-config"
    value="${user.home}/services/conf/ds-storage-application.properties"
    type="java.lang.String" override="false"/>
```

`ds-present`'s context would define its own two entries pointing at
`ds-present-behaviour.yaml`/`ds-present-application.properties` instead — two distinct files per
service, exactly like the YAML files already are, so there's no possibility of one service's secrets
leaking into another's even when they share a Tomcat instance and JVM.

This file is registered as a `PropertiesConfigSource` at ordinal **270** — deliberately *above*
SmallRye's own implicit `config/application.properties` convention (260, see the next section), so
that convention can never accidentally outrank the correct, explicitly-configured file for a given
service. Only the keys the file actually defines are affected; everything else still comes from the
YAML file:

```properties
# ${user.home}/services/conf/ds-storage-application.properties (never committed - lives outside git entirely)
db.password=the-real-production-password
db.connectionPoolSize=25
```

A template is provided at `ds-storage/conf/ds-storage-application.properties.SAMPLE` (copy it to
`ds-storage-application.properties`, without the `.SAMPLE` suffix, to the path your
`application-properties-config` entry points at, and fill in real values). Both the JNDI entry and
the argument are optional: if the entry isn't defined, `ContextListener` logs that none is
configured and starts up with only the YAML file; if the entry is defined but the file it names
can't be found, `ServiceConfig` logs an **error** (not a warning) and continues without that source —
values that were meant to come from it, most importantly secrets, will be missing or fall back to
the YAML file, but the service still starts. `ServiceConfigPropertiesOverrideFileTest` demonstrates
both the successful-override and the file-not-found cases.

## The single-instance-only convenience file: config/application.properties

`config/application.properties` (note: a `config` folder, distinct from this project's own `conf`
folder used for YAML) is a plain `key=value` properties file that SmallRye Config reads
automatically as one of its built-in default sources (ordinal 260), from the current working
directory the service is started from. No code change, no extra argument to
`ServiceConfig.initialize(...)`, and no restart-time wiring is needed for it to take effect — it is
registered by the single `.addDefaultSources()` call already in `ServiceConfig.initialize(...)`.

**This is only safe to use for a single, standalone instance of the service** (e.g. an IDE run, or
`java -jar`, from a working directory nothing else shares) — it must *not* be used for real
devops/operations secrets, since a Tomcat instance hosting several WARs shares one JVM working
directory: a file left here would silently apply to every service sharing that instance, with no
way to tell them apart. Use the explicit per-service file above for that.

A template is provided at `ds-storage/config/application.properties.SAMPLE` (copy it to
`config/application.properties`, without the `.SAMPLE` suffix, and fill in your own values). The
file itself is git-ignored (`**/config/application.properties` in the repo-root `.gitignore`).

`ServiceConfigApplicationPropertiesOverrideTest` demonstrates and verifies this end-to-end: it writes
a temporary `config/application.properties` with dummy values, asserts that `ServiceConfig` picks up
its values ahead of the YAML file's, and always removes it again afterwards (there is nothing to
preserve: the real per-service secrets never live at this path, see above).

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
can still matter if `origins` is ever (partially) redefined via a higher-priority source, e.g. the
devops/operations properties override file with `origins[0].name=...`, or a runtime injection. A naive
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
(300), the explicit devops/operations properties override file (270) and the single YAML file (100).
SmallRye Config re-reads all sources on every
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
* Consider `@ConfigMapping` interfaces for strongly-typed, validated config sections instead of raw
  `getValue(String, Class)` calls, for sections that don't need runtime mutability.
* Decide whether runtime-injected properties should be reachable via an admin API and, if so, what
  authorization/audit trail that needs.
