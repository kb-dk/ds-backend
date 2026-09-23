# =============================================================================
#  ds-backend image build
#
#    deps            - ds-shared, the three contract modules and ds-kaltura.
#                      Built ONCE and inherited by every service stage below, so
#                      `docker compose build` compiles them a single time.
#    build-<service> - one service, on top of deps. Compiles that module and
#                      nothing else; its upstreams are already in the deps layer.
#    <service>       - the runtime image.
#
#  There is no whole-reactor stage any more. `docker compose build ds-present`
#  compiles ds-present and its contracts, not the other six services.
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE: deps - shared library and contracts, built once for everyone
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /build

# -Dmaven.repo.local=/m2 is load-bearing: a BuildKit cache mount is NOT part of
# the image, so artifacts installed into a mounted /root/.m2/repository would not
# survive into this layer for the child stages to inherit. A real directory does,
# and the Docker layer cache then does the job the mount was doing.
ENV MVN="mvn -B --settings /run/secrets/maven_settings -Dmaven.repo.local=/m2"
ENV DEPS="ds-shared,ds-storage-api,ds-license-api,ds-present-api,ds-kaltura"

COPY maven_settings_security_relocation.xml /root/.m2/settings-security.xml

# --- poms first: this layer survives every source-only change ---
COPY pom.xml .
COPY ds-shared/pom.xml       ds-shared/
COPY bff/pom.xml       bff/
COPY ds-storage-api/pom.xml  ds-storage-api/
COPY ds-license-api/pom.xml  ds-license-api/
COPY ds-present-api/pom.xml  ds-present-api/
COPY ds-storage/pom.xml  ds-storage/
COPY ds-license/pom.xml  ds-license/
COPY ds-present/pom.xml  ds-present/
COPY ds-kaltura/pom.xml      ds-kaltura/
COPY ds-datahandler/pom.xml      ds-datahandler/
COPY ds-discover/pom.xml      ds-discover/
COPY ds-image/pom.xml      ds-image/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl $DEPS dependency:go-offline

# --- contract sources: change rarely, so this is usually a cache hit ---
COPY ds-storage-api/.openapi-codegen-ignore-api ds-storage-api/
COPY ds-license-api/.openapi-codegen-ignore-api ds-license-api/
COPY ds-present-api/.openapi-codegen-ignore-api ds-present-api/
COPY ds-shared/src       ds-shared/src
COPY ds-storage-api/src  ds-storage-api/src
COPY ds-license-api/src  ds-license-api/src
COPY ds-present-api/src  ds-present-api/src
COPY ds-kaltura/src      ds-kaltura/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl $DEPS install -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-storage
# -----------------------------------------------------------------------------
FROM deps AS build-ds-storage
COPY ds-storage/pom.xml ds-storage/
COPY ds-storage/.openapi-codegen-ignore-* ds-storage/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-storage dependency:go-offline
COPY ds-storage/src ds-storage/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-storage package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-license
# -----------------------------------------------------------------------------
FROM deps AS build-ds-license
COPY ds-license/pom.xml ds-license/
COPY ds-license/.openapi-codegen-ignore-* ds-license/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-license dependency:go-offline
COPY ds-license/src ds-license/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-license package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-present
# -----------------------------------------------------------------------------
FROM deps AS build-ds-present
COPY ds-present/pom.xml ds-present/
COPY ds-present/.openapi-codegen-ignore-* ds-present/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-present dependency:go-offline
COPY ds-present/src ds-present/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-present package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-datahandler
# -----------------------------------------------------------------------------
FROM deps AS build-ds-datahandler
COPY ds-datahandler/pom.xml ds-datahandler/
COPY ds-datahandler/.openapi-codegen-ignore-* ds-datahandler/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-datahandler dependency:go-offline
COPY ds-datahandler/src ds-datahandler/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-datahandler package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-discover
# -----------------------------------------------------------------------------
FROM deps AS build-ds-discover
COPY ds-discover/pom.xml ds-discover/
COPY ds-discover/.openapi-codegen-ignore-* ds-discover/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-discover dependency:go-offline
COPY ds-discover/src ds-discover/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-discover package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: ds-image
# -----------------------------------------------------------------------------
FROM deps AS build-ds-image
COPY ds-image/pom.xml ds-image/
COPY ds-image/.openapi-codegen-ignore-* ds-image/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-image dependency:go-offline
COPY ds-image/src ds-image/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl ds-image package -DskipTests

# -----------------------------------------------------------------------------
# BUILD: bff
# -----------------------------------------------------------------------------
FROM deps AS build-bff
COPY bff/pom.xml bff/
COPY bff/.openapi-codegen-ignore-* bff/
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl bff dependency:go-offline
COPY bff/src bff/src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    $MVN -pl bff package -DskipTests

# -----------------------------------------------------------------------------
# RUNTIME: ds-storage
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-storage
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-storage-configs

COPY --from=build-ds-storage /build/ds-storage/target/*.war /usr/local/tomcat/webapps/ds-storage.war
COPY conf/ds-storage/local/ds-storage.logback.xml .
COPY conf/ds-storage/local/ds-storage-devel.template.yaml .
COPY conf/ds-storage/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-storage/local/ds-storage.xml /usr/local/tomcat/conf/Catalina/localhost/ds-storage.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: ds-license
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-license
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-license-configs

COPY --from=build-ds-license /build/ds-license/target/*.war /usr/local/tomcat/webapps/ds-license.war
COPY conf/ds-license/local/ds-license.logback.xml .
COPY conf/ds-license/local/ds-license-devel.template.yaml .
COPY conf/ds-license/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-license/local/ds-license.xml /usr/local/tomcat/conf/Catalina/localhost/ds-license.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: ds-present
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-present
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-present-configs

COPY --from=build-ds-present /build/ds-present/target/*.war /usr/local/tomcat/webapps/ds-present.war
COPY conf/ds-present/local/ds-present.logback.xml .
COPY conf/ds-present/local/ds-present-devel.template.yaml .
COPY conf/ds-present/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-present/local/ds-present.xml /usr/local/tomcat/conf/Catalina/localhost/ds-present.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: ds-datahandler
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-datahandler
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-datahandler-configs

COPY --from=build-ds-datahandler /build/ds-datahandler/target/*.war /usr/local/tomcat/webapps/ds-datahandler.war
COPY conf/ds-datahandler/local/ds-datahandler.logback.xml .
COPY conf/ds-datahandler/local/ds-datahandler-devel.template.yaml .
COPY conf/ds-datahandler/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-datahandler/local/stage_preservica_dr_arkiv.txt ./oai.timestamps/
COPY conf/ds-datahandler/local/ds-datahandler.xml /usr/local/tomcat/conf/Catalina/localhost/ds-datahandler.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: ds-discover
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-discover
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-discover-configs

COPY --from=build-ds-discover /build/ds-discover/target/*.war /usr/local/tomcat/webapps/ds-discover.war
COPY conf/ds-discover/local/ds-discover.logback.xml .
COPY conf/ds-discover/local/ds-discover-devel.template.yaml .
COPY conf/ds-discover/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-discover/local/ds-discover.xml /usr/local/tomcat/conf/Catalina/localhost/ds-discover.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: ds-image
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-image
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/ds-image-configs

COPY --from=build-ds-image /build/ds-image/target/*.war /usr/local/tomcat/webapps/ds-image.war
COPY conf/ds-image/local/ds-image.logback.xml .
COPY conf/ds-image/local/ds-image-devel.template.yaml .
COPY conf/ds-image/local/entrypoint.sh /entrypoint.sh
COPY conf/ds-image/local/ds-image.xml /usr/local/tomcat/conf/Catalina/localhost/ds-image.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: bff
# -----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin-jammy AS bff
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/tomcat/conf/bff-configs

COPY --from=build-bff /build/bff/target/*.war /usr/local/tomcat/webapps/bff.war
COPY conf/bff/local/bff.logback.xml .
COPY conf/bff/local/bff-devel.template.yaml .
COPY conf/bff/local/entrypoint.sh /entrypoint.sh
COPY conf/bff/local/bff.xml /usr/local/tomcat/conf/Catalina/localhost/bff.xml

RUN chmod +x /entrypoint.sh
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# -----------------------------------------------------------------------------
# RUNTIME: solr   (config comes out of the ds-present build)
# -----------------------------------------------------------------------------
FROM solr:9.4.0 AS solr
COPY --chown=solr:solr --from=build-ds-present /build/ds-present/target/solr/dssolr/conf /opt/solr/user_config/conf
COPY --chown=solr:solr conf/solr/init-solr.sh /docker-entrypoint-initdb.d/init-solr.sh
COPY --chown=solr:solr --from=build-ds-present /build/ds-present/src/main/solr/solr.xml /opt/solr-9.4.0/server/solr/solr.xml

RUN chmod +x /docker-entrypoint-initdb.d/init-solr.sh
