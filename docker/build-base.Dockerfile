# =============================================================================
#  ds-backend build base: every module pom, plus ds-shared, the three contract
#  modules and ds-kaltura compiled and installed into /m2.
#
#  It holds third-party dependencies AND our own shared code, compiled: a base
#  that the service build stages start from, not just a dependency cache.
#
#  Not built on its own. docker-compose.yml builds it as the `build-base` helper
#  service and hands it to every service Dockerfile as the named build context
#  `build-base`, which is why each of those starts `FROM build-base` without
#  defining it.
#  Compose builds this once per `docker compose build`, however many services
#  reference it.
#
#  Build context: the repository root.
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS build-base
WORKDIR /build

# The Maven repository at /m2 is split in two (aether.enhancedLocalRepository.split):
#   /m2/installed  what we build and `install` here: ds-shared, contracts, ds-kaltura.
#                  A real directory, so it is part of this image and every service
#                  stage that starts FROM build-base inherits it.
#   /m2/cached     everything downloaded from Nexus. A BuildKit cache mount
#                  (id ds-backend-m2), shared by this file and all service builds and
#                  kept between builds, like the old Dockerfile's ~/.m2 mount. A pom or
#                  contract change therefore recompiles, but never re-downloads.
#                  If the cache is ever pruned, Maven simply downloads again.
# The service builds run in parallel against the same cache, so resolution uses
# file locks in the shared mount instead of Maven's default in-JVM locks.
# -nsu (no snapshot updates): our own modules are all 100.0.0-SNAPSHOT, and Jenkins
# deploys snapshots of them to Nexus. Without -nsu, a service build would take a
# newer Nexus snapshot of a contract over the one just built from this working tree.
# There are no external SNAPSHOT dependencies today; if one is ever added, note that
# Docker builds will not refresh it.
# git-commit-id-maven-plugin is skipped in image builds. It needs a .git directory,
# and the only way to give it one is COPY .git .git - which changes on every commit
# and would invalidate the cache this Dockerfile exists to preserve. It used to work
# only because the old builder did `COPY . .`. Images therefore carry no git metadata
# in their build.properties; tag the IMAGE with the commit instead, which is the more
# useful place for it anyway.
ENV MVN="mvn -B -nsu --settings /run/secrets/maven_settings -Dmaven.repo.local=/m2 \
         -Daether.enhancedLocalRepository.split=true \
         -Daether.syncContext.named.factory=file-lock \
         -Daether.syncContext.named.nameMapper=file-gav \
         -Daether.syncContext.named.basedir.locksDir=/m2/cached/.locks \
         -Dmaven.gitcommitid.skip=true -Dgit.failOnNoGitDirectory=false"
# The modules compiled into this base: everything a service build needs from us.
ENV SHARED_MODULES="ds-shared,ds-storage-api,ds-license-api,ds-present-api,ds-kaltura"

COPY maven_settings_security_relocation.xml /root/.m2/settings-security.xml

# All twelve poms: the reactor root lists every module, so -pl needs them present.
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

# Sources of the shared modules. Changing any of them rebuilds this layer, and with
# it every service image - but from the download cache, so it costs compile time only.
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
    --mount=type=cache,id=ds-backend-m2,target=/m2/cached \
    $MVN -pl $SHARED_MODULES install -DskipTests
