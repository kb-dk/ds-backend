# =============================================================================
#  ds-present
#
#  Build context: the repository root (the service needs its conf/ from there).
#  `build-base` and `tomcat-base` are named build contexts supplied by
#  docker-compose.yml - they are not stages in this file, so building it with a
#  bare `docker build` fails by design. Use `docker compose build ds-present`.
# =============================================================================

# -----------------------------------------------------------------------------
# build: this module only. Its poms, contracts and dependencies are already in
# the build-base image; $MVN and /build come with it.
# -----------------------------------------------------------------------------
FROM build-base AS build
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-present dependency:go-offline
# The whole module, not just src: assembly descriptors reach into conf/ and other
# per-module folders, and enumerating those service by service is a trap.
# dependency:go-offline has already run above against the poms inherited from the
# build-base layer, so the expensive layer is cached before this point. `**/target/` in
# .dockerignore keeps the module's local build output out of this copy.
COPY ds-present/ ds-present/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-present package -DskipTests

# -----------------------------------------------------------------------------
# runtime
# -----------------------------------------------------------------------------
FROM tomcat-base AS runtime
WORKDIR /usr/local/tomcat/conf/ds-present-configs

COPY --from=build /build/ds-present/target/*.war /usr/local/tomcat/webapps/ds-present.war
COPY conf/ds-present/local/ds-present.logback.xml .
COPY conf/ds-present/local/ds-present-devel.template.yaml .
COPY conf/ds-present/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-present/local/ds-present.xml /usr/local/tomcat/conf/Catalina/localhost/ds-present.xml

RUN chmod +x /entrypoint.sh

# -----------------------------------------------------------------------------
# solr: its configuration comes out of the ds-present build, so it lives here
# as a second target. docker-compose.yml builds it with `target: solr`.
# -----------------------------------------------------------------------------
FROM solr:9.4.0 AS solr
COPY --chown=solr:solr --from=build /build/ds-present/target/solr/dssolr/conf /opt/solr/user_config/conf
COPY --chown=solr:solr conf/solr/init-solr.sh /docker-entrypoint-initdb.d/init-solr.sh
COPY --chown=solr:solr --from=build /build/ds-present/src/main/solr/solr.xml /opt/solr-9.4.0/server/solr/solr.xml

RUN chmod +x /docker-entrypoint-initdb.d/init-solr.sh
