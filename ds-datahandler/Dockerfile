# =============================================================================
#  ds-datahandler
#
#  Build context: the repository root (the service needs its conf/ from there).
#  `deps` and `tomcat-base` are named build contexts supplied by
#  docker-compose.yml - they are not stages in this file, so building it with a
#  bare `docker build` fails by design. Use `docker compose build ds-datahandler`.
# =============================================================================

# -----------------------------------------------------------------------------
# build: this module only. Its poms, contracts and dependencies are already in
# the deps image; $MVN and /build come with it.
# -----------------------------------------------------------------------------
FROM deps AS build
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-datahandler dependency:go-offline
# The whole module, not just src: assembly descriptors reach into conf/ and other
# per-module folders, and enumerating those service by service is a trap.
# dependency:go-offline has already run above against the poms inherited from the
# deps layer, so the expensive layer is cached before this point; .dockerignore
# keeps target/ out.
COPY ds-datahandler/ ds-datahandler/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-datahandler package -DskipTests

# -----------------------------------------------------------------------------
# runtime
# -----------------------------------------------------------------------------
FROM tomcat-base AS runtime
WORKDIR /usr/local/tomcat/conf/ds-datahandler-configs

COPY --from=build /build/ds-datahandler/target/*.war /usr/local/tomcat/webapps/ds-datahandler.war
COPY conf/ds-datahandler/local/ds-datahandler.logback.xml .
COPY conf/ds-datahandler/local/ds-datahandler-devel.template.yaml .
COPY conf/ds-datahandler/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-datahandler/local/stage_preservica_dr_arkiv.txt ./oai.timestamps/
COPY conf/ds-datahandler/local/ds-datahandler.xml /usr/local/tomcat/conf/Catalina/localhost/ds-datahandler.xml

RUN chmod +x /entrypoint.sh
