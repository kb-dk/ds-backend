# Independent module builds: ds-storage pilot

A working demo of splitting one service into a contract module and a service
module, and of building a single service image without compiling the other
eight. Everything here is additive and reversible: the aggregate build still
works, and the seven services that have not been split are untouched.

## Apply it

    git checkout -b demo/independent-builds
    ./apply-split.sh
    mvn clean install -DskipTests

`apply-split.sh` only does `git mv` of four things into the new module. Every
other change is already in the poms and the Dockerfile.

## What changed

    ds-storage-api/            NEW - spec, generated DTOs, DsStorageClient
    ds-storage/                the war; generates its own JAX-RS interfaces, DTOs from the contract
    pom.xml                    ds-storage-api added to <modules>, before ds-storage
    ds-license/pom.xml         dk.kb.storage:ds-storage:jar:api -> ds-storage-api
    ds-present/pom.xml         same
    ds-datahandler/pom.xml     same
    Dockerfile                 deps stage + per-service stage + legacy stage
    .dockerignore              .git excluded
    docker-compose.yml         unchanged - stage names are the same

No Java source changed. The DTO package is still `dk.kb.storage.model.v1`, so
no import in any consumer moves.

## The demo

Three things worth showing, in this order.

**1. The contract builds on its own, in seconds.**

    mvn -pl ds-storage-api clean install

It depends on ds-shared and nothing else in the repo. Before the split, getting
the same artifact meant building the whole of ds-storage: generating sources,
compiling the service, packaging the war.

**2. A consumer builds without the producer's service code.**

    mvn -pl ds-present -am -DskipTests package

Watch the reactor list. ds-present now needs ds-storage-api, not ds-storage.
The service module it used to drag in is simply absent.

**3. One image, without compiling the other eight.**

    docker compose build ds-storage       # deps + ds-storage only
    docker compose build ds-present       # still the whole reactor, not yet split

The contrast between those two commands is the argument. Run them twice to show
the second run of `ds-storage` hitting the deps cache.

Then show that nothing was lost:

    docker compose build                  # all images, deps stage built once

## Points worth making while it runs

The clients were never the problem. The dependency was always there; it just
used to live in a URL string instead of a pom, where the compiler could not see
it. What blocked independent builds was that the api jar was a side-artifact of
the war, so producing the contract required building the service.

Blast radius. Before: a compile error in bff failed the ds-storage image. After:
`docker compose build ds-storage` does not compile bff at all.

The aggregate build is not sacrificed. The deps stage is built once and shared by
every service stage, so `docker compose build` is no slower than it was. This was
the real risk of going per-service, and it is handled.

Versioning did not change. Everything is still `100.0.0-SNAPSHOT`, develop is
still "the absolute latest", and no version needs bumping to add a field to a
DTO. That conversation is genuinely separate and can wait.

Where the boundary actually falls. The contract module holds the DTOs and the
client. The generated JAX-RS *interfaces* stay with the service, because
jaxrs-cxf-extended annotates them with @KBAuthorization from the service's own
webservice package - and a client has no use for a server interface anyway. The
service still generates those interfaces on every build, but takes its DTOs from
the contract jar.

## Verification checklist

This was written without being able to run Maven, so treat the first build as
the test. In likely order of failure:

1. `ds-storage-api` compiles. If `DsStorageClient` imports `dk.kb.storage.api.v1.*`
   the model-only split will not hold, and `KBAuthorization` has to move to
   ds-shared instead. If it references a plain helper left in the service module,
   `IdNormaliser` is the candidate - move it across the same way.
2. `ds-storage` compiles. Its impl classes now implement interfaces from the api
   jar rather than from its own generated sources.
3. The war still serves the spec at `/ds-storage/api/openapi.yaml`. The spec is
   unpacked from the api jar into `target/classes` by maven-dependency-plugin.
4. ds-license, ds-present and ds-datahandler compile against the new coordinate.
5. The api jar's dependency list is deliberately over-inclusive - it was copied
   from the service and had only the database and integration-test entries
   removed. Trimming it to what the client actually needs is a good follow-up and
   makes the contract jar genuinely thin.
