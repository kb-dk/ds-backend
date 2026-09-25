# =============================================================================
#  Tomcat runtime base shared by every ds-backend service image.
#
#  These lines were previously repeated verbatim in all seven runtime stages.
#  Patch Tomcat here, once. Handed to each service Dockerfile as the named build
#  context `tomcat-base` by docker-compose.yml.
#
#  Contract with the service images: each one copies its own /entrypoint.sh and
#  makes it executable; ENTRYPOINT and CMD below are inherited unchanged.
# =============================================================================
FROM tomcat:9.0-jdk17-temurin-jammy

# envsubst, used by every service's entrypoint.sh to render its config template
RUN apt-get update && apt-get install -y gettext-base && rm -rf /var/lib/apt/lists/*

# Tomcat tweaks: the services pass encoded slashes in path parameters
RUN echo "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true" >> /usr/local/tomcat/conf/catalina.properties
RUN sed -i 's/<Connector port="8080"/<Connector port="8080" encodedSolidusHandling="passthrough"/' /usr/local/tomcat/conf/server.xml

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
