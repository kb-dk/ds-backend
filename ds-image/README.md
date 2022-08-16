# ds-image

Wrapper for an image server (IIPImage) extending with upload capabilities 

Developed and maintained by the Royal Danish Library.

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

The default port is 8103 and the default Hello World service can be accessed at
<http://localhost:8080/ds-image/v1/hello>

The Swagger UI is available at <http://localhost:8103/ds-image/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
