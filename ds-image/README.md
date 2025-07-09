# ds-image

Wrapper for an image server (IIPImage) extending with upload capabilities 

Developed and maintained by the Royal Danish Library.

## ⚠️ Warning: Copyright Notice
Please note that it is not permitted to download and/or otherwise reuse content from the DR-archive at The Danish Royal Library.


## Requirements

* Maven 3                                  
* Java 17

## Setup

**PostgreSQL database creation, Solr installation etc. goes here**

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 9077 and the default Hello World service can be accessed at
<http://localhost:9077/ds-image/v1/hello>

The Swagger UI is available at <http://localhost:9077/ds-image/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

## Using a client to call the service 
This project produces a support JAR containing client code for calling the service from Java.
This can be used from an external project by adding the following to the [pom.xml](pom.xml):
```xml
<!-- Used by the OpenAPI client -->
<dependencies>
  <dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.2</version>
  </dependency>

  <dependency>
    <groupId>dk.kb.image</groupId>
    <artifactId>ds-image</artifactId>
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
</dependencies>
```
after this a client can be created with
```java
    DsImageClient imageClient = new DsImageClient("https://example.com/ds-image/v1");
```
During development, a SNAPSHOT for the OpenAPI client can be installed locally by running
```shell
mvn install
```


See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
