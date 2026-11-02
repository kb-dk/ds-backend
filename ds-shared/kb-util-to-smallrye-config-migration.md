# kb-util YAML → SmallRye Config migration (ds-backend)

This is the single migration guide for the project-wide replacement of kb-util's `YAML`-backed
configuration with [SmallRye Config](https://smallrye.io/smallrye-config/) (a standalone
MicroProfile Config implementation — this project does not use Quarkus or Spring). It lives in
`ds-shared` because that's where the generic parts of the pattern actually sit: the vendored
`dk.kb.util` property-loading code (`Resolver`, the legacy `YAML` class), `OpenApiResource`'s
`Config`-based placeholder substitution, and the generic test (`ApplicationPropertiesOverrideTest`)
that demonstrates the override mechanism independent of any one module's `ServiceConfig`.

It replaces two earlier, now-deleted documents that drifted out of date and out of sync with each
other: `ds-shared/SMALLRYE_CONFIG_MIGRATION.md` (a design reference) and
`ds-storage/SMALLRYE_CONFIG_MIGRATION.md` (an early ds-storage-only proof-of-concept write-up).
There is intentionally only one migration guide in the repo, kept here.

**Status as of 2026-10-07: migration complete for all eight modules** — `ds-shared`, `ds-storage`,
`ds-license`, `ds-image`, `ds-discover`, `ds-present`, `ds-datahandler`, and `bff`. `ds-present` was
additionally refactored to a DTO-based config design (see below); `bff` has one piece of behaviour
(auto-reload) none of the others need (see below). The sections below were each re-verified against
the current source in this repo, not just carried over from prior notes.

## Module status

| Module | Status | Notes |
|---|---|---|
| ds-shared | Done | Generic `ApplicationPropertiesOverrideTest` + `OpenApiResource`'s `${config:...}` wildcard placeholder support (both `[*]` list-index and bare `*` dynamic/dotted-key wildcards). |
| ds-storage | Done | Standard single-file pattern; the worked example throughout this doc. |
| ds-license | Done | Standard pattern. Also fixed an unrelated flaky test (`RightsModuleFacadeTest`) by adding an `ID DESC` tiebreaker to `AuditLogModuleStorage`'s `ORDER BY MODIFIEDTIME` queries (millisecond-resolution ties). |
| ds-image | Done | Standard pattern. |
| ds-discover | Done | Standard pattern. |
| ds-present | Done, then further refactored | See "ds-present: DTO-based ServiceConfig design" below. `ServiceConfig` deliberately keeps both a nested `YAML` tree and a flattened `Config` view side by side — this is by design, not leftover migration debt (see that section). |
| ds-datahandler | Done | Standard pattern. |
| bff | Done | See "bff: auto-reloading ServiceConfig" below — the one module with genuinely different runtime behaviour, not just a different file layout. |

## Standard per-module pattern (all modules except bff's auto-reload addition)

- `ServiceConfig.getConfig()` returns MicroProfile `Config` directly (SmallRye), replacing the old
  kb-util `YAML` return type — except `ds-present`, which keeps `getConfig(): YAML` for its nested
  tree alongside a separate `getFlatConfig(): Config` (see below).
- Each module's `ServiceConfig.initialize(String configFile)` now takes **one** YAML file (or a glob
  that resolves to one logical file, per module convention — `ds-present` deliberately still globs,
  see below), resolved via `Resolver.resolveURL(...)`, rather than kb-util's old multi-file
  glob-and-layer convention.
- Ordinal scheme, highest wins: `RUNTIME_ORDINAL` = 500 (in-process runtime injection) >
  system properties (400) > environment variables (300) > an optional `.env` file (295) >
  the explicit per-service devops/operations properties override file (270) >
  SmallRye's implicit `config/application.properties` (260) > classpath `application.properties`
  (250) > the module's YAML file (100, registered as a custom `MapConfigSource`/`YamlConfigSource`).
- Per-module JNDI: `application-config` (the YAML file) and `application-properties-config` (the
  devops override file), both looked up in `ContextListener` and declared in `conf/ocp/<module>.xml`
  (production/Tomcat) and `src/test/jetty/jetty-env.xml` (local Jetty runs).
- `KBOAuth2Handler.java` (present in `ds-datahandler`, `ds-present`, `ds-image`, `ds-storage`,
  `ds-discover`, `ds-license`) scans `Config.getPropertyNames()` for `"security."`-prefixed names in
  a for-each loop (`Iterable<String>`, not `Collection` — `.stream()` isn't available), reads scalars
  via `getOptionalValue`, and uses a local indexed-list helper for keys like `security.realms[0]`.
- Each `ServiceConfig` also exposes `setRuntimeProperty`/`clearRuntimeProperty`/
  `getRuntimePropertyNames` (see "Runtime property injection" below).
- `.properties` files with real secrets are never created or committed — only `.properties.SAMPLE`
  placeholder templates (per this project's data-compliance rules). The one documented exception is
  `ds-shared/config/application.properties` itself — see the next section.

## The generic test: ds-shared's ApplicationPropertiesOverrideTest

`ds-shared/src/test/java/dk/kb/util/webservice/ApplicationPropertiesOverrideTest.java` demonstrates
and verifies the `config/application.properties` override mechanism end-to-end, generically, once —
this isn't specific to any one module's YAML schema, so it's tested here rather than once per module.
(Note: an earlier draft of this guide referenced it under a `dk.kb.util.config` package; the real,
current package is `dk.kb.util.webservice`, matching `OpenApiResource`'s own package.)

Unlike every other module, where `config/application.properties` must never be committed (it's the
local/operational override file and can carry real secrets, so it stays covered by this repo's
blanket `**/config/application.properties` `.gitignore` rule), `ds-shared/config/application.properties`
is **committed directly to the repository** and must be present on every checkout:

- `ds-shared` is a library module — it is never deployed standalone, so this file can never carry a
  real production secret the way another module's could.
- Its values are dummy test fixtures only (e.g. a placeholder `db.password`, a `db.connectionPoolSize`,
  and a small `oaiTargets[0]`/`oaiTargets[1]` list used to exercise indexed-property overriding).
- The repo-root `.gitignore` has an explicit exception, `!ds-shared/config/application.properties`,
  carved out of the blanket rule for exactly this reason.
- There is no `.SAMPLE` template for it (unlike every other module) — the real file *is* the
  checked-in template, since there is nothing in it to keep out of git.

Because the file is always expected to be present, the test **fails** (`assertTrue`, not
`Assumptions.assumeTrue`) if it's missing, rather than silently skipping — a missing file on a fresh
checkout means something is wrong (e.g. the checkout is shallow/sparse, or the file was deleted by
mistake), and a loud failure surfaces that immediately instead of a silently-skipped test giving a
false sense of coverage.

## The real devops/operations override file: an explicit per-service path

A Tomcat instance can host several `ds-backend` WARs at once (the development server runs all
services in one Tomcat), while production gives each service its own instance. That rules out
relying on anything keyed off the JVM process itself (a system property, an environment variable, or
a file found via the shared current working directory) to carry a *per-service* secret such as a
database password — every webapp in a shared Tomcat instance would see the same value.

The fix mirrors how the YAML file itself already avoids this: `application-config` is a per-webapp
Tomcat context `<Environment>` entry, so each module points at its own YAML file even while sharing a
Tomcat instance. The properties override file gets the same treatment via a second, optional context
entry, `application-properties-config`, and a second, optional argument to `ServiceConfig.initialize(...)`:

```java
public static synchronized void initializeWithPropertiesOverride(String configFile, String propertiesOverrideFile) throws IOException
```

In `conf/ocp/ds-storage.xml`, for example:

```xml
<Environment name="application-config"
    value="${user.home}/services/conf/ds-storage-behaviour.yaml"
    type="java.lang.String" override="false"/>
<Environment name="application-properties-config"
    value="${user.home}/services/conf/ds-storage-application.properties"
    type="java.lang.String" override="false"/>
```

Each other module's context defines its own two entries pointing at its own
`<module>-behaviour.yaml`/`<module>-application.properties` — distinct files per service, so there's
no possibility of one service's secrets leaking into another's even when they share a Tomcat instance
and JVM. (`bff` uses a different path convention for these — `/app/conf/bff-base.yaml` /
`/app/conf/bff-application.properties` rather than `${user.home}/services/conf/...` — a known,
unreconciled inconsistency, not a bug.)

This file is registered as a `PropertiesConfigSource` at ordinal **270** — deliberately *above*
SmallRye's own implicit `config/application.properties` convention (260), so that convention can
never accidentally outrank the correct, explicitly-configured file for a given service. A template is
provided at `<module>/conf/<module>-application.properties.SAMPLE` in every module (copy it,
dropping `.SAMPLE`, to the path the `application-properties-config` entry points at, and fill in real
values — never commit the result). Both the JNDI entry and the argument are optional: if the entry
isn't defined, startup logs that none is configured and continues with only the YAML file; if it's
defined but the file can't be found, `ServiceConfig` logs an **error** (not a warning) and continues
without that source. Each module's own `ServiceConfigPropertiesOverrideFileTest` exercises both the
successful-override and file-not-found cases (confirmed present in `ds-storage`, `ds-license`,
`ds-image`, `ds-discover`, `ds-datahandler`, and `ds-present`).

## The single-instance-only convenience file: config/application.properties

`config/application.properties` (a `config` folder, distinct from this project's `conf` folder used
for YAML) is a plain `key=value` file SmallRye Config reads automatically as a built-in default
source (ordinal 260), from the working directory the service is started from — no extra argument to
`initialize(...)` needed.

**This is only safe for a single, standalone instance of a service** (an IDE run, `java -jar`, a
working directory nothing else shares) — it must not be used for real devops/operations secrets,
since a Tomcat instance hosting several WARs shares one JVM working directory: a file left here would
silently apply to every service sharing that instance. Use the explicit per-service file above for
that. Every module except `ds-shared` keeps this file git-ignored with a `.SAMPLE` template to copy
from; `ds-shared`'s own copy is the one documented exception (see above).

## YAML stays almost the same

Each `<module>-behaviour.yaml` keeps its structure and keys unchanged. One line per module typically
needed to change because of a genuine syntax difference between kb-util's extrapolation (Apache
Commons Text `StringSubstitutor`) and SmallRye Config's own `${...}` expression syntax:

```diff
- url: jdbc:h2:${env:TMPDIR:-/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
+ url: jdbc:h2:${TMPDIR:/tmp}/h2_ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

Environment variables are already a config source in SmallRye Config (300), so `${TMPDIR}` resolves
directly against the env var; `${TMPDIR:/tmp}` adds the same `/tmp` fallback the old `:-` syntax
provided.

## Known semantic difference: overriding list values

SmallRye Config flattens a YAML list into indexed properties (`origins[0].name`, `origins[1].name`,
...), each resolved independently — highest-ordinal source wins per key, same as any scalar. kb-util's
old merge instead replaced the *entire* list as soon as an overriding file redefined it at all.

Now that each `ServiceConfig` loads a single YAML file, this rarely comes up — but it can still matter
if `origins` is redefined via a higher-priority source (the devops override file, or a runtime
injection): a naive per-index read would mix entries from both sources instead of one cleanly
replacing the other. `ds-storage`'s `ServiceConfig.loadAllowedOrigins()` special-cases this: it finds
the single highest-ordinal source that defines *any* `origins[...]` entry and reads the whole list
from that source alone, so one source always wins outright for `origins`. This is not a generic
solution — a future list-valued config needing the same treatment should apply the same pattern, or
use a `@ConfigMapping` with an explicit `List<T>` and accept SmallRye's per-index override semantics.

## Runtime property injection

Each `ServiceConfig` registers a small in-memory `ConfigSource` at the highest ordinal (500) of all
sources used — above system properties, environment variables, the devops override file, and the
YAML file:

```java
ServiceConfig.setRuntimeProperty("db.connectionPoolSize", "42"); // takes effect immediately
ServiceConfig.clearRuntimeProperty("db.connectionPoolSize");     // reverts to the YAML value
```

SmallRye Config re-reads all sources on every `getValue`/`getOptionalValue` call rather than caching a
snapshot, so a runtime override is visible immediately with no restart. `setRuntimeProperty` redacts
values in its log line for keys whose name looks like a password/secret/token. See each module's own
`ServiceConfigRuntimeInjectionTest` (confirmed present, e.g. in `ds-storage`) for a runnable
demonstration. This stays at the Java API level today — there is no HTTP/admin endpoint exposing it.

## ds-present: DTO-based ServiceConfig design

`ds-present`'s configuration includes an `origins`/`storages`/`views`/`transformers` object graph
whose shape (nested, with data-driven cardinality) has no direct equivalent in SmallRye Config's flat
property model. Rather than forcing that tree through `Config`, `ServiceConfig` deliberately keeps
**two** parallel representations side by side, and is the *only* class in the module allowed to touch
either `YAML` or MicroProfile `Config` directly — every other class works only with plain DTOs or
primitives returned by `ServiceConfig`:

- `getConfig(): YAML` — the original kb-util tree (`YAML.resolveLayeredConfigs(...)`), including the
  multi-file glob merge (`ds-present-behaviour.yaml` + `ds-present-kb-origins.yaml` + an optional
  environment-specific file) and `${path:...}` self-referencing extrapolation. Internal to
  `ServiceConfig` — only a few tests exercising generic YAML/library behaviour still touch it directly.
- `getOrigins()`, `getStorages()`, and the other typed getters — plain Java DTOs
  (`dk.kb.present.config.OriginConfig`, `StorageConfig`, `ViewConfig`, the `TransformerConfig`/
  `BackendConfig` marker-interface families) built on demand by a dedicated `ConfigParser` class from
  the `YAML` tree above. This is how every other class in `ds-present` reads configuration.
- `getFlatConfig(): Config` — a MicroProfile `Config`, used only where a third-party/MicroProfile-aware
  API needs the raw object itself (the OpenAPI endpoint, `KBOAuth2Handler`). Built by flattening the
  merged `YAML` tree (including the nested sections, for the `OpenApiResource` wildcard substitution
  to work against it) and layering it under runtime injection, system properties, environment
  variables, `.env`, and the devops override file.

So: `ds-present` is **not** carrying leftover, unmigrated `YAML` usage — the `dk.kb.util.yaml.YAML`
import in its `ServiceConfig.java` is a deliberate, documented part of the design (confirmed by
reading the current class javadoc and source), not migration debt.

Follow-up notes from this refactor, for anyone touching it again:
- Scalar getters (`getLicenseModuleUrl()`, `isUseTranscriptionsEnabled()`, `isStopOnErrorEnabled()`,
  `getRecordIdPattern()`, `getOriginPrefixPattern()`) read from `getFlatConfig()`/`getConfig()`
  directly on `ServiceConfig`; all nested-tree parsing (`parseOrigins`/`parseStorages`/...) lives in
  the separate `ConfigParser` class, which takes the already-loaded `YAML` tree as a parameter (no
  call-back into `ServiceConfig`, so no circular dependency).
- When changing a factory/handler constructor's parameter type in a refactor like this, diff the new
  `throws` clause against the original line by line — narrowing to no checked exception compiles fine
  against an interface declaring `throws Exception`, and only fails at the call site.
- Grep test sources for direct calls to the changed method/constructor names, not just for the old
  type being removed — tests that build a factory or config-consumer directly from a hand-built
  fixture, bypassing `ServiceConfig`, won't show up in a `ServiceConfig`-usage grep.

## bff: auto-reloading ServiceConfig

`bff`'s config (`dk.kb.oauth.config.ServiceConfig`) differs from every other module in one respect:
operators edit `bff-base.yaml` directly in production to change user-facing content (most notably the
`messages.*` keys shown on the frontend, and `secretSalt`, used by `EncryptionHelper` to encrypt/
decrypt the BFF cookie) and these must take effect without a service restart — replicating the old
kb-util `AutoYAML` behaviour.

- A background daemon thread (`ServiceConfig-autoupdate`) rebuilds the entire `SmallRyeConfig` from
  the same `configFile`/`propertiesOverrideFile` paths every `config.autoupdate.intervalms`
  milliseconds (default 60000) and atomically swaps a `volatile` field, so readers never see a
  half-rebuilt config. Controlled by `config.autoupdate.enabled`/`intervalms`, read from the config
  itself; restarting the auto-update is idempotent, so re-`initialize()`-ing (as tests do) never leaks
  threads. A reload failure is caught, logged, and the last known-good config keeps serving.
- `getMessagesConfig()` reconstructs the `messages.*` section as a flat `Map<String, Object>` by
  scanning `Config.getPropertyNames()` for that prefix (MicroProfile `Config` has no "read back a
  sub-tree" operation the way `YAML.getSubMap()` did); every call reflects the current, possibly
  auto-reloaded, config.
- `ContextListener.contextInitialized` calls `ServiceConfig.initialize(...)`; `contextDestroyed` stops
  the thread cleanly.
- **Known gap, still open**: `bff/conf/bff-base.yaml` currently has no example `messages:` block, even
  though `getMessagesConfig()` and the real production YAML use one — worth adding a sample block so a
  fresh local run doesn't silently get an empty messages map.
- Checked for this guide: no leftover `AutoYAML`-based class remains anywhere in `bff` (or any other
  module) — every `AutoYAML` reference left in the codebase is a javadoc/comment pointing back at the
  retired kb-util mechanism for context, not live code.

## Recurring failure pattern worth knowing about

Files that called kb-util-YAML-only methods (`.getSubMap()`, `.containsKey()`, `.getString()`,
`.getList()`, `.getInteger()`) directly against `ServiceConfig.getConfig()` broke compilation once
that method's return type changed from `YAML` to `Config` (or, for `ds-present`, once most callers
were moved onto DTOs instead of either). This surfaced more than once per module as files missed in
the initial migration pass. When doing a similar signature-changing refactor, grep test sources too —
not just production code — for direct calls to the changed method/constructor names, since a test that
builds a factory or config object directly from a hand-built fixture bypasses a grep for the old type
name entirely.

## Remaining work

None blocking. Two items are worth a look but are not confirmed problems:

- `bff/conf/bff-base.yaml` has no sample `messages:` block (see above).
- `bff`'s `/app/conf/...` JNDI path convention hasn't been reconciled with every other module's
  `${user.home}/services/conf/...` convention — may be intentional, not yet confirmed either way.

Possible future improvements, not required by anything above: `@ConfigMapping` interfaces for
strongly-typed, validated config sections instead of raw `getValue(String, Class)` calls where runtime
mutability isn't needed; and deciding whether runtime-injected properties should be reachable via an
admin API (none exists today — `setRuntimeProperty`/`clearRuntimeProperty` are Java-API-only).
