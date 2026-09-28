# =============================================================================
#  ds-storage
#
#  Build context: the repository root (the service needs its conf/ from there).
#  `build-base` and `tomcat-base` are named build contexts supplied by
#  docker-compose.yml - they are not stages in this file, so building it with a
#  bare `docker build` fails by design. Use `docker compose build ds-storage`.
# =============================================================================

# -----------------------------------------------------------------------------
# build: this module only. Its poms, contracts and dependencies are already in
# the build-base image; $MVN and /build come with it.
# -----------------------------------------------------------------------------
FROM build-base AS build
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-storage dependency:go-offline
# The whole module, not just src: assembly descriptors reach into conf/ and other
# per-module folders, and enumerating those service by service is a trap.
# dependency:go-offline has already run above against the poms inherited from the
# build-base layer, so the expensive layer is cached before this point. `**/target/` in
# .dockerignore keeps the module's local build output out of this copy.
COPY ds-storage/ ds-storage/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-storage package -DskipTests

# -----------------------------------------------------------------------------
# runtime
# -----------------------------------------------------------------------------
FROM tomcat-base AS runtime
WORKDIR /usr/local/tomcat/conf/ds-storage-configs

COPY --from=build /build/ds-storage/target/*.war /usr/local/tomcat/webapps/ds-storage.war
COPY conf/ds-storage/local/ds-storage.logback.xml .
COPY conf/ds-storage/local/ds-storage-devel.template.yaml .
COPY conf/ds-storage/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-storage/local/ds-storage.xml /usr/local/tomcat/conf/Catalina/localhost/ds-storage.xml

RUN chmod +x /entrypoint.sh
