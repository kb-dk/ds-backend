# =============================================================================
#  ds-backend build base: every module pom, plus ds-shared, the three contract
#  modules and ds-kaltura compiled and installed into /m2.
#
#  Not built on its own. docker-compose.yml builds it as the `ds-deps` helper
#  service and hands it to every service Dockerfile as the named build context
#  `deps`, which is why each of those starts `FROM deps` without defining it.
#  Compose builds this once per `docker compose build`, however many services
#  reference it.
#
#  Build context: the repository root.
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /build

# -Dmaven.repo.local=/m2 is load-bearing: a BuildKit cache mount is NOT part of
# the image, so artifacts installed into a mounted /root/.m2/repository would not
# survive into this layer for the child stages to inherit. A real directory does,
# and the Docker layer cache then does the job the mount was doing.
# git-commit-id-maven-plugin is skipped in image builds. It needs a .git directory,
# and the only way to give it one is COPY .git .git - which changes on every commit
# and would invalidate the cache this Dockerfile exists to preserve. It used to work
# only because the old builder did `COPY . .`. Images therefore carry no git metadata
# in their build.properties; tag the IMAGE with the commit instead, which is the more
# useful place for it anyway.
ENV MVN="mvn -B --settings /run/secrets/maven_settings -Dmaven.repo.local=/m2 \
         -Dmaven.gitcommitid.skip=true -Dgit.failOnNoGitDirectory=false"
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
