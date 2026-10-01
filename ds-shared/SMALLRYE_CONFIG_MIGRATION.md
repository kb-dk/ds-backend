# ds-backend: ServiceConfig migrated to SmallRye Config

This document describes the project-wide replacement of the kb-util `YAML`-backed `ServiceConfig`
with [SmallRye Config](https://smallrye.io/smallrye-config/) (a standalone MicroProfile Config
implementation — this project does not use Quarkus). It lives in `ds-shared` because `ds-shared` is
where the generic parts of this pattern actually sit: the vendored `dk.kb.util` property-loading
code (`Resolver`, the legacy `YAML` class, etc.), `OpenApiResource`'s `Config`-based placeholder
substitution, and the generic tests that demonstrate the mechanism independent of any one module's
`ServiceConfig`.

**Scope:** every `ds-backend` module has been migrated — `ds-storage`, `ds-datahandler`,
`ds-discover`, `ds-image`, `ds-license`, `ds-present`, and `bff`. Each module keeps its own
`ServiceConfig` class (the public API — `getConfig()`, module-specific typed getters,
`initialize(...)`) but they all follow the identical pattern documented below. `ds-storage` is used
as the worked example throughout since it was the first module converted; substitute the module's
own name for its YAML/properties file names (`ds-storage-behaviour.yaml` → `ds-present-behaviour.yaml`,
etc.). `bff` additionally has a background auto-reload thread on top of this base pattern — see
"bff: background auto-reload" near the end.

For current per-module migration status and a devel-server deployment checklist, see the project's
own `kb-util-to-smallrye-config-migration.md` and `devel-deployment-checklist.md` docs; this file is
the lasting design reference for *how* the mechanism itself works, independent of rollout status.

## What changed, per module

| Area | Change |
|---|---|
| `<module>/pom.xml` | Added `smallrye-config` and `smallrye-config-source-yaml` (3.12.4). |
| `<module>/src/main/java/.../config/ServiceConfig.java` | Rewritten on top of `SmallRyeConfig`. The old `initialize(String... configFiles)` (multi-file glob) became `initialize(String configFile)` (single file, resolved with `Resolver.resolveURL(...)`), plus a new `initialize(String configFile, String propertiesOverrideFile)` overload (see "The real devops override file" below). Each module's own typed getters are unchanged in signature/behaviour; each also gained `setRuntimeProperty` / `clearRuntimeProperty` / `getRuntimePropertyNames`. |
| `<module>/.../webservice/ContextListener.java` | Now also looks up an optional `application-properties-config` JNDI entry and passes it as the second argument to `ServiceConfig.initialize(...)`; logs the resolved path (or that none is configured). |
| Code reading `YAML.getSubMap(...)` / `.getString(...)` / `.getList(...)` etc. | Switched to reading keys directly from the MicroProfile `Config` (`getValue`, `getOptionalValue`, or a loop over `getPropertyNames()` for a prefix). Behaviour unchanged, including fallback/warning semantics where the original code had them. |
| `<module>/conf/<module>-behaviour.yaml` | Structure and keys unchanged; any `${env:VAR:-default}` kb-util extrapolation syntax became SmallRye's own `${VAR:default}` syntax (see "YAML stays almost the same" below). |
| `<module>/conf/ocp/<module>.xml` | The `application-config` Tomcat context entry changed from a glob (`/app/conf/<module>*.yaml`) to a single path. A new `application-properties-config` entry was added (see below). |
| `<module>/src/test/jetty/jetty-env.xml` | Same YAML change for local Jetty runs, plus a new `propertiesConfig` entry (absent by default — a local run logs an error for it, see below). |
| `<module>/config/application.properties.SAMPLE` | **New**, one per module. Template for the single-instance-only local override file (see below). |
| `<module>/conf/<module>-application.properties.SAMPLE` | **New**, one per module. Template for the real, per-service devops/operations override file (see below). |
| `.gitignore` (repo root) | Added `**/config/application.properties` and `*-application.properties` — these files carry secrets (e.g. the real db password) and must never be committed. |
| `ds-shared/pom.xml` | Added `microprofile-config-api` (interface only, no implementation). |
| `ds-shared/src/main/java/dk/kb/util/webservice/OpenApiResource.java` | `setConfig(YAML)` → `setConfig(Config)`. Still supports `${config:yaml.path}` and `${config:origins[*].name}` (wildcard) placeholders in OpenAPI specs, now resolved against MicroProfile Config instead of YAML. |
| `ds-shared/src/test/java/dk/kb/util/config/ApplicationPropertiesOverrideTest.java` | **New** (see below — this is the generic test, not tied to any one module). |

Each module also kept (unchanged, or lightly adapted to the single-file `initialize(...)` signature)
its own module-specific config tests — e.g. `ServiceConfigRuntimeInjectionTest` and
`ServiceConfigPropertiesOverrideFileTest`, which demonstrate runtime injection and the per-service
devops override file respectively, using that module's own YAML/property fixtures.

### Why touch ds-shared?

Every module's `Application_v1.getClasses()` calls `OpenApiResource.setConfig(ServiceConfig.getConfig())`
to let `OpenApiResource` (in `ds-shared`) substitute `${config:...}` placeholders in the OpenAPI spec
(e.g. `${config:openapi.serverurl}`, and the wildcard `${config:origins[*].name}` used to build the
enum of allowed origins). That method's signature is defined in `ds-shared` and previously required a
kb-util `YAML`. It was changed to accept the standard `org.eclipse.microprofile.config.Config`
interface instead — `ds-shared` only depends on the MicroProfile Config *API* (no concrete
implementation), so it stays decoupled from the choice of SmallRye Config made in each module.

## Single YAML file, not three

Every module's `ServiceConfig` previously loaded a **glob** of files (e.g. `ds-storage-behaviour.yaml`,
`ds-storage-environment.yaml`, `ds-storage-local.yaml`) resolved via kb-util's
`Resolver.resolveGlob(...)` and layered in alphanumerical order, each overriding the previous one
key by key. That convention has been retired: `ServiceConfig.initialize(String configFile)` now takes
exactly **one** YAML file, resolved with `Resolver.resolveURL(...)` (verbatim file path → classpath →
user home). In production that single path is still configured entirely outside the project, via the
same `application-config` Tomcat context environment entry as before (`conf/ocp/<module>.xml`) — it's
simply a plain path now instead of a glob. (`ds-present` is a documented exception: it still resolves
its `application-config` entry as a glob of `ds-present*.yaml`, by deliberate choice for that module.)

Environment- or operator-specific overrides that used to live in a second or third YAML file are
now expressed with SmallRye Config's own built-in layering instead, all of which sit above the
single YAML file (ordinal 100) in priority:

* system properties (ordinal 400) — e.g. `-Ddb.connectionPoolSize=42`
* environment variables (300) — e.g. `DB_CONNECTIONPOOLSIZE=42`
* an optional `.env` file (295) next to the working directory
* an optional `config/application.properties` file (260) — a plain `key=value` file, the closest
  drop-in replacement for the old `*-local.yaml`/`*-environment.yaml` pattern
* runtime injection via `ServiceConfig.setRuntimeProperty(...)` (500, highest — see below)

Each `<module>-behaviour.yaml` keeps its structure and keys unchanged (see below), and the old
`*-environment.yaml.SAMPLE` files are no longer read by `ServiceConfig` — they've been replaced by
the two override files described next.

## The real devops/operations override file: an explicit per-service path

The production/development deployment topology matters here: **a Tomcat instance can host several of
the `ds-backend` WARs at once** (the development server runs all services in one Tomcat), while
production gives each service its own Tomcat instance. That rules out relying on anything keyed off
the JVM process itself (a system property, an environment variable, or a file found via the shared
current working directory) to carry a *per-service* secret such as a database password — all webapps
in that shared Tomcat instance would see the exact same value.

The fix mirrors how the YAML file itself already avoids this problem: `application-config` is a
per-webapp Tomcat context `<Environment>` entry, so each module can point at its own YAML file even
while sharing one Tomcat instance. The properties override file gets the same treatment, via a
second, optional context entry, `application-properties-config`, and a second, optional argument to
`ServiceConfig.initialize(...)`:

```java
public static synchronized void initialize(String configFile, String propertiesOverrideFile) throws IOException
```

`ContextListener` looks up both JNDI entries and passes them straight through. In
`conf/ocp/ds-storage.xml`, for example:

```xml
<Environment name="application-config"
    value="${user.home}/services/conf/ds-storage-behaviour.yaml"
    type="java.lang.String" override="false"/>
<Environment name="application-properties-config"
    value="${user.home}/services/conf/ds-storage-application.properties"
    type="java.lang.String" override="false"/>
```

Each other module's context defines its own two entries pointing at its own
`<module>-behaviour.yaml`/`<module>-application.properties` instead — distinct files per service,
exactly like the YAML files already are, so there's no possibility of one service's secrets leaking
into another's even when they share a Tomcat instance and JVM. (`bff` uses a different path
convention for these two entries — `/app/conf/bff-base.yaml` / `/app/conf/bff-application.properties`
rather than `${user.home}/services/conf/...` — flagged as a known inconsistency, not yet reconciled.)

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

A template is provided at `<module>/conf/<module>-application.properties.SAMPLE` (copy it to
`<module>-application.properties`, without the `.SAMPLE` suffix, to the path your
`application-properties-config` entry points at, and fill in real values). Both the JNDI entry and
the argument are optional: if the entry isn't defined, `ContextListener` logs that none is
configured and starts up with only the YAML file; if the entry is defined but the file it names
can't be found, `ServiceConfig` logs an **error** (not a warning) and continues without that source —
values that were meant to come from it, most importantly secrets, will be missing or fall back to
the YAML file, but the service still starts. Each module's own
`ServiceConfigPropertiesOverrideFileTest` demonstrates both the successful-override and the
file-not-found cases.

## The single-instance-only convenience file: config/application.properties

`config/application.properties` (note: a `config` folder, distinct from this project's own `conf`
folder used for YAML) is a plain `key=value` properties file that SmallRye Config reads
automatically as one of its built-in default sources (ordinal 260), from the current working
directory the service is started from. No code change, no extra argument to
`ServiceConfig.initialize(...)`, and no restart-time wiring is needed for it to take effect — it is
registered by the single `.addDefaultSources()` call already in `ServiceConfig.initialize(...)`.

**This is only safe to use for a single, standalone instance of a service** (e.g. an IDE run, or
`java -jar`, from a working directory nothing else shares) — it must *not* be used for real
devops/operations secrets, since a Tomcat instance hosting several WARs shares one JVM working
directory: a file left here would silently apply to every service sharing that instance, with no
way to tell them apart. Use the explicit per-service file above for that.

A template is provided at `<module>/config/application.properties.SAMPLE` in every module (copy it
to `config/application.properties`, without the `.SAMPLE` suffix, and fill in your own values). The
file itself is git-ignored (`**/config/application.properties` in the repo-root `.gitignore`). Some
modules' own `ServiceConfigTest` (the "sample config" test) checks whether this file exists locally
and logs a warning if it doesn't, purely as a local-dev convenience nudge — it does not fail the
build either way.

`ds-shared/src/test/java/dk/kb/util/config/ApplicationPropertiesOverrideTest` demonstrates and
verifies this mechanism end-to-end, generically — this is the mechanism every module's
`ServiceConfig` relies on, so it's tested once, here, rather than once per module. It reads a
pre-existing, persistent `ds-shared/config/application.properties` fixture (not committed to git;
see `ds-shared/config/application.properties.SAMPLE`) and asserts its values win over a test YAML
file. If that fixture doesn't exist locally, the test is skipped (via `Assumptions.assumeTrue`), not
failed. This test previously lived in `ds-storage` as `ServiceConfigApplicationPropertiesOverrideTest`
and wrote/deleted a throwaway file at test time; it was moved here and changed to use a persistent
fixture instead, since the mechanism it demonstrates isn't specific to `ds-storage` at all.

## YAML stays almost the same

Each `<module>-behaviour.yaml` keeps its structure and keys exactly as before. One line per module
typically had to change because of a genuine syntax difference between kb-util's extrapolation
(Apache Commons Text `StringSubstitutor`) and SmallRye Config's own `${...}` expression syntax, e.g.:

```diff
- url: jdbc:h2:${env:TMPDIR:-/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
+ url: jdbc:h2:${TMPDIR:/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

Environment variables are already a config source in SmallRye Config (ordinal 300), so `${TMPDIR}`
resolves directly against the `TMPDIR` env var; `${TMPDIR:/tmp}` adds the same `/tmp` fallback the
old `:-` syntax provided. No other expression changed.

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

Where a module reads `origins` (e.g. `ServiceConfig.loadAllowedOrigins()` in `ds-storage`), it
special-cases this: it finds the single highest-ordinal config source that defines *any*
`origins[...]` entry and reads the whole list from that source alone, so one source always wins
outright for `origins`, never a per-index blend. This is not a generic solution for arbitrary YAML
lists — if a future config value needs the same treatment, apply the same pattern (or introduce a
`@ConfigMapping` with an explicit `List<T>` and accept SmallRye's per-index override semantics).

## Runtime property injection

Each `ServiceConfig` registers a small in-memory `ConfigSource` (`ServiceConfig.RuntimeConfigSource`)
at the highest ordinal (500) of all sources used — above system properties (400), environment
variables (300), the explicit devops/operations properties override file (270) and the single YAML
file (100). SmallRye Config re-reads all sources on every `getValue`/`getOptionalValue` call rather
than caching a snapshot (unlike the old `YAML`, which was immutable once loaded), so:

```java
ServiceConfig.setRuntimeProperty("db.connectionPoolSize", "42"); // takes effect immediately
ServiceConfig.getConnectionPoolSize();                           // -> 42, no restart needed
ServiceConfig.clearRuntimeProperty("db.connectionPoolSize");     // reverts to the YAML value (10)
```

See each module's own `ServiceConfigRuntimeInjectionTest` for a runnable demonstration, including
injecting a key that doesn't exist in the YAML file at all. `setRuntimeProperty` redacts values in
its log line for keys that look like passwords/secrets/tokens.

This deliberately stays at the Java API level: there is no HTTP/admin endpoint to call
`setRuntimeProperty` from outside the JVM. Adding one (e.g. a `POST /v1/admin/config` guarded by an
admin role) would be a natural next step if runtime injection needs to be operable from outside the
process.

## bff: background auto-reload

`bff` has one addition beyond the base pattern above: a background daemon thread that periodically
rebuilds the whole `SmallRyeConfig` from the same `configFile`/`propertiesOverrideFile` paths and
atomically swaps a `volatile` reference, replicating the reload behaviour kb-util's `AutoYAML`
provided. It's controlled by two config keys, read from the loaded config itself:

```yaml
config:
  autoupdate:
    enabled: true      # default: false
    intervalms: 60000   # default: 60000 (1 minute)
```

`ServiceConfig.restartAutoUpdateIfNeeded()` reads these keys and starts/stops the thread accordingly;
`isAutoUpdating()` and `shutdown()` are exposed for `ContextListener`'s lifecycle hooks. Exceptions
during a reload are caught and logged — they never kill the thread or leave `getConfig()` returning
a stale/broken reference. This only applies to `bff` today; no other module currently needs reload
without a restart.

## Suggested follow-ups

* Reconcile `bff`'s `/app/conf/...` path convention for `application-config` /
  `application-properties-config` with the `${user.home}/services/conf/...` convention every other
  module uses, or confirm it's intentionally different.
* Consider `@ConfigMapping` interfaces for strongly-typed, validated config sections instead of raw
  `getValue(String, Class)` calls, for sections that don't need runtime mutability.
* Decide whether runtime-injected properties should be reachable via an admin API and, if so, what
  authorization/audit trail that needs.
