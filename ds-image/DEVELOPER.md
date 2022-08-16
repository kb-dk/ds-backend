# Developer documentation

This project is built from the [java webapp template](https://sbprojects.statsbiblioteket.dk/stash/projects/ARK/repos/java-webapp-template/browse)
from the Royal Danish Library.

The information in this document is aimed at developers that are not proficient in the java webapp template, Jetty, 
Tomcat deployment or OpenAPI.


## Initial use

After a fresh checkout or after the `ds-image-openapi_v1.yaml` specification has changed, the `api` and the `model` files 
must be (re)generated. This is done by calling 
```
mvn package
```

Jetty is enabled, so testing the webservice can be done by running
Start a Jetty web server with the application:
```
mvn jetty:run
```

The default port is 8103 and the default Hello World service can be accessed at
<http://localhost:8103/ds-image/v1/hello>
where "ds-image" is your artifactID from above.

The Swagger UI is available at <http://localhost:8103/ds-image/api/>, providing access to the `v1` version of the GUI. 

## java webapp template config

Configuration of the project is handled with [YAML](https://en.wikipedia.org/wiki/YAML). It is split into multiple parts:
 
 * `behaviour` which contains setup for thread pools, limits for arguments etc. This is controlled by the developers.
 * `environment` which contains server names, userIDs, passwords etc. This is controlled by operations.
 * `local` which contains temporary developer overrides. This is controlled by the individual developer.

Extra configuration files can be added when needed. They are merged in alphanumeric order and later configuration
elements overrides previous ones.

Access to the merged configuration is through the static class at `src/main/java/dk.kb.image/config/ServiceConfig.java`.


### Developer config

When developing an application, there will typically be 4 files in the `conf`-folder:

* `conf/ds-image-behaviour.yaml`: Application centric config, such as limits for input parameters, rules
for transformations and thumbnail sizes  
  _Shared by everyone, controlled by developers, part of the repo._
* `conf/ds-image-environment.yaml.sample`: Sample/template for environment config, such as server names, 
passwords and thread count. Should contain key-value pairs for all possible environment config elements,
but with non-sensitive dummy-values. The sample YAML will **not** be merged with the other configs.  
  _Shared by everyone, controlled by developers, used as template by operations, part of the repo._
* `conf/ds-image-local.yaml`: Concrete environment config for the individual developer, mirroring   
`conf/ds-image-environment.yaml.sample` but with locally working values. When needed for experimental
purposes, values from the behaviour config can also be overridden here.  
  _Not shared, controlled by the individual developer, **not** part of the repo._
* `conf/ds-image-logback.xml`: File-oriented Logback config, intended for deploy in Tomcat.
Note that `mvn jetty:run` uses `src/test/jetty/logback.xml`.  
  _Shared by everyone, controlled by developers, might be adjusted by operations, part of the repo._

This setup it used when `mvn jetty:run` is called. For deployment on a devel-, stage- or production-server,
see the **Tomcat** section further down.

**Note**: The environment configuration typically contains sensitive information. Do not put it in open code
repositories! To guard against this, `conf/ds-image-environment.yaml` is added to `.gitignore`. 

## Jetty

Jetty is a servlet container (like Tomcat) that is often used for testing during development.

This project can be started with `mvn jetty:run`, which will expose a webserver with the implemented service at port 8103.
If it is started in debug mode from an IDE (normally IntelliJ IDEA), breakpoints and all the usual debug functionality
will be available.

Running under Jetty will result in the configs _behaviour_ and _local_ being used.

## Tomcat

Tomcat is the default servlet container for the Royal Danish Library and as deployment to Tomcat must be tested before
delivering the project to Operations. As of 2021, Tomcat 9 is used for Java 11 applications.

A [WAR](https://en.wikipedia.org/wiki/WAR_(file_format))-file is generated with `mvn package` and can be deployed
directly into Tomcat, although this will log to `catalina.out` and use the developer configuration YAML.

Deployment on a shared server or a developer machine:

* Copy `conf/<application-ID>-behaviour.yaml` to the config folder on the server, which is typically
  `$HOME/services/conf`, `$HOME/services/<application-ID>/` or `$HOME/conf/`.
* Copy `conf/<application-ID>-environment.yaml.sample` to the config folder on the server, rename the file to
   `<application-ID>-environment.yaml` and adjust the values to fit the concrete environment.
* Copy the generated WAR to the designated folder on the server, probably `$HOME/services/tomcat-apps/`.
* Adjust the paths to `docBase` (the path to the recently copied WAR), `<application-ID>.yaml` and `<application-ID>-logback.xml` in a copy of `conf/ocp/<application-ID>.xml` and copy it to the designated folder on the server, probably `$HOME/services/tomcat-apps/`.
* Symlink (`ln -s`) the file `<application-ID>.xml` to the Tomcat folder `conf/Catalina/localhost/`

Deployment on a production server at the Royal Danish Library is normally done by Operations and is normally quite
similar to the procedure for test- and developer-machines. The delivery aimed at Operations is the tar-ball generated
as part of the maven build. The tar-ball is the artifact with the suffix `-distribution.tar.gz` found in the target folder
and also uploaded to nexus as part of the release procedure. The tar-ball contains:

* The WAR-file
* Readme
* Changelog
* `<application-ID>-behaviour.yaml` (should be used as-is)
* `<application-ID>.environment.yaml.sample` (sample environment config, to be renamed and adjusted by Operations)
* `<application-ID>-logback.xml` (might be adjusted by Operations)
* `<application-ID>.xml.sample` (sample tomcat context, to be renamed and adjusted by Operations)

## Tests

Unit tests are run using the surefire plugin (configured in the parent pom).

If you have unit tests that takes long to run, and don't want them to run when at every invocation of mvn package,
annotate the testcase with `@Tag("slow")` in the java code. 
To run all unit tests including the ones tagged as slow, enable the `allTests` maven profile: e.g. `mvn clean package -PallTests`.

## A full web application

For smaller projects or standalone web applications, it can be useful to bundle the user interface with the API 
implementation: Files and folders added to the `src/main/webapp/` folder are served under 
[http://localhost:8103/<application-ID>/](http://localhost:8103/<application-ID>/).

While it is possible to use [JSP](https://en.wikipedia.org/wiki/Jakarta_Server_Pages), as the sample 
[index.jsp](./src/main/webapp/index.jsp) shows, this is considered legacy technology.
With an [API first](http://apievangelist.com/2020/03/09/what-is-api-first/) approach, the web application
will typically be static files and JavaScript.

## OpenAPI 1.3 (aka Swagger)

[OpenAPI 1.3](https://swagger.io/specification/) generates interfaces and skeleton code for webservices.
It also generates online documentation, which includes sample calls and easy testing of the endpoints.

Everything is defined centrally in the file [src/main/openapi/ds-image-openapi_v1.yaml](src/main/openapi/ds-image-openapi_v1.yaml).
IntelliJ IDEA has a plugin for editing OpenAPI files that provides a semi-live preview of the generated GUI and
the online [Swagger Editor](https://editor.swagger.io/) can be used by copy-pasting the content of `ds-image-openapi_v1.yaml`.


The interfaces and models generated from the OpenAPI definition are stored in `target/generated-sources/`.
They are recreated on each `mvn package`.

Skeleton classes are added to `/src/main/java/${project.package}/api/v1/impl/` but only if they are not already present. 
A reference to the classes must be present in `/src/main/java/${project.package}/webservice/Application` or its equivalent.

A common pattern during initial definition of the `ds-image-openapi_v1.yaml` is to delay the implementation phase and recreate 
the skeleton implementation files on each build. This can be done by setting `generateOperationBody` in the `pom.xml` 
to `true`.

**Tip:** If the `ds-image-openapi_v1.yaml` is changed a lot during later development of the application, it might be better to have 
`<generateOperationBody>true</generateOperationBody>` in `pom.xml` and add the implementation code to manually created
classed (initially copied from the OpenAPI-generated skeleton impl classes). When changes to `ds-image-openapi_v1.yaml` results in
changed skeleton implementation classes, the changes can be manually ported to the real implementation classes.

**Note:** The classes in `/src/main/java/${project.package}api/impl/` will be instantiated for each REST-call.
Persistence between calls must be handled as statics or outside of the classes.

### OpenAPI and exceptions

When an API end point shall return anything else than the default response (HTTP response code 200),
this is done by throwing an exception.

See how we map exceptions to responsecodes in [ServiceExceptionMapper](./src/main/java/dk/kb/webservice/ServiceExceptionMapper.java) 

See [ServiceException](./src/main/java/dk/kb/webservice/exception/ServiceException.java) and its specializations for samples.

### Mustache templates

The templates in [src/main/templates/](./src/main/templates/) overrides the default Swagger templates.
They are needed in order to provide functionality needed by the Royal Danish Library, e.g. delivering a streaming
response while announcing a well-defined structure in the Swagger UI.

Normally you do not need to touch the Mustache-files.

### API versioning

The project comes with a single version: `v1`. Current best practise is "try and avoid new versions", meaning that
_additions_ to APIs in production (both new methods and in responses) should generally not trigger a new version.

When a project hase been deployed in production and new functionality requires breaking the API contract, developers
should

  * Create a new OpenAPI YAML, e.g. `ds-image-openapi_v2.yaml` (preferably by copying `ds-image-openapi_v1.yaml` and adjusting) and place
    it alongside `ds-image-openapi_v1.yaml`
    * Edit the old `ds-image-openapi_v1.yaml` and add a note that a new version is available
  * Locate the `openapi-generator-maven-plugin` section in `pom.xml` and add setup for the new version (copy-paste 
    the two `execution` blocks for Version 1 and adjust accordingly)
  * Create a new `Application` version alongside `webservice/Application_v1.java` (copy the old one and adjust the
    `*ServiceImpl` import)
  * Add a new `servlet` and a new `servlet-mapping` for the new `Application` in `src/main/webapp/WEB-INF/web.xml`  
  * Add the path to the new OpenAPI YAML to `urls` in `webapp/api/index.html`. The first entry in the array is the
    default when visiting the main API-page at <http://localhost:8103/ds-image/api/>  

After a `mvn package`, a skeleton implementation for the new version of the API class will be created in the source
tree. The standard action is to copy the implementation for the previous version to the new one and adjust from there.  


## Changelog

The changelog follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) guidelines.
The trickiest part is to get the diff-links correct at the bottom of CHANGELOG.md as the git *tags* used for references
should be created *after* the CHANGELOG has been updated.

For GitHub-projects, the syntax is
```
[Unreleased](https://github.com/kb-dk/ds-image-template/compare/v1.0.0...HEAD)
[1.1.0](https://github.com/kb-dk/ds-image-template/compare/v1.0.0...v1.1.0)
[1.0.0](https://github.com/kb-dk/ds-image-template/releases/tag/v1.0.0)
```

For BitBucket (KB's internal git), the syntax is
```
[Unreleased](https://sbprojects.statsbiblioteket.dk/stash/projects/ARK/repos/ds-image-template/compare/commits?targetBranch=refs%2Ftags%2Fv1.1.0&sourceBranch=refs%2Fheads%2Fmaster)
[1.1.0](https://sbprojects.statsbiblioteket.dk/stash/projects/ARK/repos/ds-image-template/compare/commits?targetBranch=refs%2Ftags%2Fv1.0.0&sourceBranch=refs%2Ftags%2Fv1.1.0)
[1.0.0](https://sbprojects.statsbiblioteket.dk/stash/projects/ARK/repos/ds-image-template/commits?until=refs%2Ftags%2Fv1.0.0)
```
Note that both the `ARK`-part and the repo-id `ds-image-template` is project-specific.


## Release procedure

1. Review that the `version` in `pom.xml` is fitting. `ds-image` uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html): The typical release
will bump the `MINOR` version and set `PATCH` to 0. Keep the `-SNAPSHOT`-part as
the Maven release plugin handles that detail.   
1. Ensure that [CHANGELOG.md](CHANGELOG.md) is up to date. `git log` is your friend. 
Ensure that the about-to-be-released version is noted in the changelog entry
1. Ensure all local changes are committed and pushed.
1. Ensure that your local `.m2/settings.xml` has a current `sbforge-nexus`-setup
(contact Kim Christensen @kb or another Maven-wrangler for help)
1. Follow the instructions on
[Guide to using the release plugin](https://maven.apache.org/guides/mini/guide-releasing.html)
which boils down to
   * Run `mvn clean release:prepare`
   * Check that everything went well, then run `mvn clean release:perform`
   * Run `git push`   
   If anything goes wrong during release, rollback and delete tags using something like
   `mvn release:rollback ; git tag -d ds-image-1.4.2 ; git push --delete origin ds-image-1.4.2`
