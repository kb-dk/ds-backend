# ds-present

Transforming access layer for metadata. Intended as a proxy for [ds-storage](https://github.com/kb-dk/ds-storage) prividing multiple views on metadata, such as MODS, JSON-LD and SolrXMLDocument.

Developed and maintained by the Royal Danish Library.

## Status

This project is at its very early stage. It does not do anything yet, besides acting af a foundation for discussions.

## Requirements

* Maven 3                                  
* Java 11
* A running [ds-storage](https://github.com/kb-dk/ds-storage)

## Setup

copy `conf/ds-present-behaviour.yaml` to `conf/ds-present-environment.yaml` and adjust URLs for the storages in the new file.

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The Swagger UI is available at <http://localhost:9073/ds-present/api/>. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
