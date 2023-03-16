# ds-discover

Gateway for Solr text search, image similarity, sound location and other discovery technologies.

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3
* Java 11

## Setup


## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 9074 and the default Hello World service can be accessed at
<http://localhost:9074/ds-discover/v1/hello>

The Swagger UI is available at <http://localhost:9074/ds-discover/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

## Using a client to call the service 
This project produces a support JAR containing client code for calling the service from Java.
This can be used from an external project by adding the following to the [pom.xml](pom.xml):
```xml
<!-- Used by the OpenAPI client -->
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.2</version>
</dependency>

<dependency>
    <groupId>dk.kb.discover</groupId>
    <artifactId>ds-discover</artifactId>
    <version>1.0-SNAPSHOT</version>
    <type>jar</type>
    <classifier>classes</classifier>
    <!-- Do not perform transitive dependency resolving for the OpenAPI client -->
    <exclusions>
        <exclusion>
          <groupId>*</groupId>
          <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
after this a client can be created with
```java
    DsDiscoverClient discoverClient = new DsDiscoverClient("https://example.com/ds-discover/v1");
```
During development, a SNAPSHOT for the OpenAPI client can be installed locally by running
```shell
mvn install
```


See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
