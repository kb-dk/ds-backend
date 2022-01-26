# ds-present

Transforming access layer for metadata. Intended as a proxy for [ds-storage](https://github.com/kb-dk/ds-storage) prividing multiple views on metadata, such as MODS, JSON-LD and SolrXMLDocument.

Developed and maintained by the Royal Danish Library.

## Status

This project is at its very early stage. It does not do anything yet, besides acting af a foundation for discussions.

## Requirements

* Maven 3                                  
* Java 11

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 8080 and the default Hello World service can be accessed at
<http://localhost:8080/ds-present/v1/hello>

The Swagger UI is available at <http://localhost:8080/ds-present/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
