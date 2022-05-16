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

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
