# =============================================================================
#  ds-image
#
#  Build context: the repository root (the service needs its conf/ from there).
#  `build-base` and `tomcat-base` are named build contexts supplied by
#  docker-compose.yml - they are not stages in this file, so building it with a
#  bare `docker build` fails by design. Use `docker compose build ds-image`.
# =============================================================================

# -----------------------------------------------------------------------------
# build: this module only. Its poms, contracts and dependencies are already in
# the build-base image; $MVN and /build come with it.
# -----------------------------------------------------------------------------
FROM build-base AS build
# The whole module, not just src: assembly descriptors reach into conf/ and other
# per-module folders, and enumerating those service by service is a trap.
# `**/target/` in .dockerignore keeps the module's local build output out of this copy.
COPY ds-image/ ds-image/
# Downloads come from the shared cache mount (see build-base.Dockerfile), so there is
# no separate dependency:go-offline step: nothing is downloaded twice either way.
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    --mount=type=cache,id=ds-backend-m2,target=/m2/cached \
    $MVN -pl ds-image package -DskipTests

# -----------------------------------------------------------------------------
# runtime
# -----------------------------------------------------------------------------
FROM tomcat-base AS runtime
WORKDIR /usr/local/tomcat/conf/ds-image-configs

COPY --from=build /build/ds-image/target/*.war /usr/local/tomcat/webapps/ds-image.war
COPY conf/ds-image/local/ds-image.logback.xml .
COPY conf/ds-image/local/ds-image-devel.template.yaml .
COPY conf/ds-image/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-image/local/ds-image.xml /usr/local/tomcat/conf/Catalina/localhost/ds-image.xml

RUN chmod +x /entrypoint.sh
