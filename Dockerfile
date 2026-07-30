# STAGE 1: The big mono-repo power mvn build (it's morphin time)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY . .

# Maven always looks at `/root/.m2/settings-security.xml` and when using Docker secrets, it need to look in
# `/run/secrets/maven_settings_security`, so the `maven_settings_security_relocation.xml` helps with that.
COPY maven_settings_security_relocation.xml /root/.m2/settings-security.xml

# Run test first, so we always run unittest when building
# Build the project using a BuildKit cache mount for Maven dependencies.
# This prevents Maven from downloading the internet on every single build.
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    --mount=type=cache,target=/root/.m2/repository \
    mvn --settings "/run/secrets/maven_settings" clean test

# Build the project using a BuildKit cache mount for Maven dependencies.
# This prevents Maven from downloading the internet on every single build.
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=maven_settings_security \
    --mount=type=cache,target=/root/.m2/repository \
    mvn --settings "/run/secrets/maven_settings" clean install -DskipTests


# STAGE 2: Runtime for ds-storage
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-storage

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-storage-configs

# Copy files
COPY --from=builder /build/ds-storage/target/*.war /usr/local/tomcat/webapps/ds-storage.war
COPY --from=builder /build/conf/ds-storage/local/ds-storage.logback.xml .
COPY --from=builder /build/conf/ds-storage/local/ds-storage-devel.template.yaml .
COPY --from=builder /build/conf/ds-storage/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY conf/ds-storage/local/ds-storage.xml /usr/local/tomcat/conf/Catalina/localhost/ds-storage.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 3: Runtime for ds-license
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-license

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-license-configs

# Copy files
COPY --from=builder /build/ds-license/target/*.war /usr/local/tomcat/webapps/ds-license.war
COPY --from=builder /build/conf/ds-license/local/ds-license.logback.xml .
COPY --from=builder /build/conf/ds-license/local/ds-license-devel.template.yaml .
COPY --from=builder /build/conf/ds-license/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/ds-license/local/ds-license.xml /usr/local/tomcat/conf/Catalina/localhost/ds-license.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 4: Runtime for ds-present
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-present

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-present-configs

# Copy files
COPY --from=builder /build/ds-present/target/*.war /usr/local/tomcat/webapps/ds-present.war
COPY --from=builder /build/conf/ds-present/local/ds-present.logback.xml .
COPY --from=builder /build/conf/ds-present/local/ds-present-devel.template.yaml .
COPY --from=builder /build/conf/ds-present/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/ds-present/local/ds-present.xml /usr/local/tomcat/conf/Catalina/localhost/ds-present.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 5: Runtime for ds-datahandler
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-datahandler

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-datahandler-configs

# Copy files
COPY --from=builder /build/ds-datahandler/target/*.war /usr/local/tomcat/webapps/ds-datahandler.war
COPY --from=builder /build/conf/ds-datahandler/local/ds-datahandler.logback.xml .
COPY --from=builder /build/conf/ds-datahandler/local/ds-datahandler-devel.template.yaml .
COPY --from=builder /build/conf/ds-datahandler/local/entrypoint.sh /entrypoint.sh
COPY --from=builder /build/conf/ds-datahandler/local/stage_preservica_dr_arkiv.txt ./oai.timestamps/

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/ds-datahandler/local/ds-datahandler.xml /usr/local/tomcat/conf/Catalina/localhost/ds-datahandler.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 6: Runtime for ds-discover
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-discover

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-discover-configs

# Copy files
COPY --from=builder /build/ds-discover/target/*.war /usr/local/tomcat/webapps/ds-discover.war
COPY --from=builder /build/conf/ds-discover/local/ds-discover.logback.xml .
COPY --from=builder /build/conf/ds-discover/local/ds-discover-devel.template.yaml .
COPY --from=builder /build/conf/ds-discover/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/ds-discover/local/ds-discover.xml /usr/local/tomcat/conf/Catalina/localhost/ds-discover.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 7: Runtime for ds-image
FROM tomcat:9.0-jdk17-temurin-jammy AS ds-image

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/ds-image-configs

# Copy files
COPY --from=builder /build/ds-image/target/*.war /usr/local/tomcat/webapps/ds-image.war
COPY --from=builder /build/conf/ds-image/local/ds-image.logback.xml .
COPY --from=builder /build/conf/ds-image/local/ds-image-devel.template.yaml .
COPY --from=builder /build/conf/ds-image/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/ds-image/local/ds-image.xml /usr/local/tomcat/conf/Catalina/localhost/ds-image.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]


# STAGE 8: Runtime for bff
FROM tomcat:9.0-jdk17-temurin-jammy AS bff

# Install envsubst
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Set working directory for configs
WORKDIR /usr/local/tomcat/conf/bff-configs

# Copy files
COPY --from=builder /build/bff/target/*.war /usr/local/tomcat/webapps/bff.war
COPY --from=builder /build/conf/bff/local/bff.logback.xml .
COPY --from=builder /build/conf/bff/local/bff-devel.template.yaml .
COPY --from=builder /build/conf/bff/local/entrypoint.sh /entrypoint.sh

# CRITICAL: Copy the XML to the specific directory Tomcat monitors
COPY --from=builder /build/conf/bff/local/bff.xml /usr/local/tomcat/conf/Catalina/localhost/bff.xml

RUN chmod +x /entrypoint.sh

# Tomcat Tweaks
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]

# STAGE 9: Runtime for solr
FROM solr:9.4.0 AS solr

COPY --chown=solr:solr --from=builder /build/ds-present/target/solr/dssolr/conf /opt/solr/user_config/conf
COPY --chown=solr:solr --from=builder /build/conf/solr/init-solr.sh /docker-entrypoint-initdb.d/init-solr.sh
COPY --chown=solr:solr --from=builder /build/ds-present/src/main/solr/solr.xml /opt/solr-9.4.0/server/solr/solr.xml

RUN chmod +x /docker-entrypoint-initdb.d/init-solr.sh