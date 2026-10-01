# kb-util YAML → SmallRye Config migration (ds-backend)

Status as of 2026-10-01: **all modules except `bff` are migrated, building, and committed. ds-present has additionally been refactored to a DTO-based config design (see below) and the user has committed this to a branch on GitHub.**

## Scope / standing instruction
Replace kb-util YAML-based configuration with SmallRye Config (standalone, non-Quarkus, v3.12.4) across the ds-backend multi-module Maven project. `bff` is explicitly deferred/excluded — not started.

## Module status
| Module | Status | Notes |
|---|---|---|
| ds-shared | Done | Shared `OpenApiResource` generalized to support `${config:...}` wildcard placeholders with both `[*]` (list index) and bare `*` (dynamic/dotted YAML key). Added test dependencies (ds-shared had none before). |
| ds-storage | Done | Standard single-file SmallRye Config pattern. |
| ds-license | Done | Standard pattern. Also fixed an unrelated flaky test (`RightsModuleFacadeTest`) by adding `ID DESC` tiebreaker to `AuditLogModuleStorage` ORDER BY MODIFIEDTIME queries (millisecond-resolution ties). |
| ds-image | Done | Standard pattern. |
| ds-discover | Done | Standard pattern. |
| ds-present | Done, then further refactored | See "ds-present: DTO-based ServiceConfig design" below — this superseded the original hybrid `getConfig()`/`getFlatConfig()` design. |
| ds-datahandler | Done | Standard pattern. Two files were missed in the initial pass and fixed afterward: `KBOAuth2Handler.java` and `OaiHarvestClientIntegrationTest.java` (both had leftover `YAML conf = ServiceConfig.getConfig()` + YAML-only method calls against what's now a `Config`). |
| bff | **Not started** | Explicitly deferred per standing instruction. |

## Standard per-module pattern (all except ds-present)
- `ServiceConfig.getConfig()` returns MicroProfile `Config` directly (SmallRye).
- Ordinal scheme: RUNTIME_ORDINAL=500 > system properties(400) > env vars(300) > .env(295) > PROPERTIES_OVERRIDE_ORDINAL=270 (devops properties override file) > implicit config/application.properties(260) > classpath application.properties(250) > YAML_ORDINAL=100.
- Per-module JNDI: `application-config` and `application-properties-config` (devops override file), in `conf/ocp/<module>.xml` and `src/test/jetty/jetty-env.xml`.
- `KBOAuth2Handler.java` rewrite pattern used in every module that has one: constructor scans config for `"security."`-prefixed property names via a for-each loop over `Config.getPropertyNames()` (`Iterable<String>`, not `Collection` — `.stream()` doesn't work), reads scalars via `getOptionalValue`, and uses a local `getIndexedStringList(Config, String)` helper for indexed list keys like `security.realms[0]`, `[1]`, ...
- `.properties` files with real secrets are never created — only `.properties.SAMPLE` placeholder templates (per data-compliance rules).

## ds-present: DTO-based ServiceConfig design (2026-10-01 refactor)
Following the standard migration above, the user asked for a further decoupling step specific to ds-present: make `ServiceConfig` the **only** class that touches `dk.kb.util.yaml.YAML` or MicroProfile `Config`; every other class in the module uses only primitives or plain Java DTOs returned by `ServiceConfig`. Rationale (user's words): "By only using get'er in the ServiceConfig, this will decouple how the properties was loaded from the rest of the code."

- Scalar settings get typed getters on `ServiceConfig`, e.g. `getLicenseModuleUrl()`, `isUseTranscriptionsEnabled()`, `isStopOnErrorEnabled()`, `getRecordIdPattern()`, `getOriginPrefixPattern()`.
- Tree-shaped settings get a new `dk.kb.present.config` package of plain DTOs, built lazily (not cached, not built at `initialize()` time — several tests `initialize()` with fixtures that lack an `origins`/`storages` section entirely):
  - `OriginConfig`, `ViewConfig`, `StorageConfig`.
  - `TransformerConfig` (marker interface) with `XsltConfig` (shared by `xslt`/`xsltsolr`), `ReplaceConfig`, `FailTransformerConfig`, `EmptyTransformerConfig` (used for `identity`/`imagerights` and as the fallback for unrecognized types — deliberately **not** `FailTransformerConfig`, so an unknown type still fails with a correct diagnostic naming the real type rather than being silently misrouted to the fail factory).
  - `BackendConfig` (marker interface) with `DsStorageBackendConfig`, `FolderBackendConfig`, `FailBackendConfig`, and an anonymous fallback (same "preserve real type name" reasoning as above).
  - `ServiceConfig.getOrigins()` / `getStorages()` return `List<OriginConfig>` / `List<StorageConfig>`, built via private `parseOrigin`/`parseView`/`parseTransformer`/`parseStorage`/`parseBackend` helpers that wrap `NotFoundException` into a friendly `IllegalArgumentException` (both `parseOrigin` and `parseStorage` do this consistently).
- Every consumer was changed to take DTOs/primitives instead of `YAML`: `StorageHandler`, `StorageController`, `StorageFactory` + its 3 implementations (`DSStorageFactory`, `FileStorageFactory`, `FailStorageFactory`), `DSOrigin`, `View`, `OriginHandler`, `TransformerController`, `DSTransformerFactory` + its 6 implementations (`XSLTFactory`, `XSLTSolrFromSchemaFactory`, `ReplaceFactory`, `IdentityFactory`, `FailFactory`, `ImageRightsFactory`), `AccessUtil`, `PresentFacade`.
- `getFlatConfig(): Config` was deliberately **kept** (not eliminated) since it's a standard MicroProfile interface already handed directly to third-party/MicroProfile-aware code (`Application_v1`'s `OpenApiResource.setConfig(...)`, `KBOAuth2Handler`).
- Deliberately left untouched: `ServiceConfigTest.testImageserverAbstraction2` and `ServiceConfigPropertiesOverrideFileTest` (both exercise generic kb-util YAML mechanics / override-file mechanism unrelated to this DTO shape), `TestFileProvider.java`.

### Compile errors found after the fact (all fixed, all worth watching for in future similar refactors)
1. **Variable shadowing across types**: `OriginHandler`'s constructor took `String originPrefixPattern` (matching the field name `Pattern originPrefixPattern`) and compiled it to a local `Pattern`, but a lambda inside the constructor referenced the unqualified name and silently resolved to the `String` parameter instead of the field — compiler caught it (`cannot find symbol: method matcher(String)`) only because the types differed. Fix: never let a constructor parameter share a name with a differently-typed field when a lambda in the same constructor needs the field; use a distinctly-named local (e.g. `compiledOriginPrefixPattern`).
2. **Dropped `throws` clause on an interface override**: rewriting `XSLTFactory`/`XSLTSolrFromSchemaFactory` to the new `TransformerConfig` parameter type lost the original `throws IOException` (the `XSLTTransformer`/`XSLTSolrFromSchemaTransformer` constructors read+compile an XSLT resource and can throw it). The interface declares `throws Exception`, so an override narrowing to no throws clause compiles fine on its own — the error only surfaces at the call site inside the method body. Fix: when rewriting a factory/handler method signature, diff the new `throws` clause against the pre-refactor original line by line, don't just match the interface's declared exception.
3. **Test helpers that call a factory or config-consumer directly, bypassing `ServiceConfig`**: several test files (`ReplaceTransformerTest`, `XSLTTransformerTestBase`, `XSLTCumulusToSchemaDotOrgTransformerTest`, `XSLTCumulusToSolrTransformerTest`, `EmbeddedSolrTest`) built a raw YAML string inline and called `someFactory.createTransformer(yaml)` or `TestUtil.getTransformedFromConfigWithAccessFields(yaml, ...)` directly, rather than going through `ServiceConfig.initialize(...)` + `getOrigins()`/`getStorages()`. These don't show up in a grep for `ServiceConfig` usage, so they were missed in the first rewrite pass and only surfaced via the user's `mvn test-compile` output. Fix: build the relevant DTO (`XsltConfig`, `ReplaceConfig`, etc.) directly with its constructor instead of parsing YAML, except where the test's own readable YAML-string mini-DSL is worth keeping for readability (e.g. `ReplaceTransformerTest.getReplacer` still parses a short YAML snippet but now constructs a `ReplaceConfig` from the parsed fields before calling the factory).

### Verification caveat
None of this could be compiled against real dependencies in the cloud sandbox used to do the rewrite — Maven Central and the internal KB Nexus are both blocked by the organization's egress policy there. Verification was manual (cross-referencing original constructor signatures) plus a classpath-less `javac` syntax-only pass. All three bug classes above were only caught by the user's actual local Maven build (`mvn test-compile`) — a good reminder that this kind of refactor needs a real local build/test cycle, not just sandbox review, before being trusted.

## Recurring failure pattern worth knowing about
Files that call kb-util-YAML-only methods (`.getSubMap()`, `.containsKey()`, `.getString()`, `.getList()`, `.getInteger()`) directly against `ServiceConfig.getConfig()` broke compilation once that method's return type changed from `YAML` to `Config`. This happened more than once per module (missed files from the initial pass, surfaced later by the user's build). Useful technique: compare file `mtimeMs` (via the device bridge) against files known to have been touched by the migration — an old/original timestamp reliably indicates a file was never updated and is worth grepping for `YAML` / `.getConfig()`.

The ds-present DTO refactor above surfaced the same general class of problem one level deeper: it's not enough to grep for the old type (`YAML`) in production code — test code that directly instantiates factories/handlers with hand-built config bypasses that grep entirely. When doing this kind of signature-changing refactor, also grep test sources for direct calls to the changed method names (`createTransformer(`, `createStorage(`, the constructor names of the classes being changed), not just for the old type being removed.

## Remaining work
- `bff` module: not started, per standing instruction (deliberately last).
- No other known outstanding compile/test issues as of this writing — user confirmed ds-present "everything is building and compiling" and has committed the DTO refactor to a branch on GitHub.
