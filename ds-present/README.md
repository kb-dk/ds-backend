# ds-present

Transforming access layer for metadata. Acts as a proxy for [ds-storage](https://github.com/kb-dk/ds-storage) 
providing multiple views on metadata, such as MODS, JSON-LD and SolrJsonDocument.

Developed and maintained by the Royal Danish Library.

## Status

This project is at the "first real attempt"-status: Connections to ds-storage and simple 1-input-1-output
transformations are working.

## Requirements

* Maven 3                                  
* Java 11
* For production or integration test: A running [ds-storage](https://github.com/kb-dk/ds-storage)

## Test run

Build and start the service using jetty with
```
mvn package jetty:run
```
After this, the Swagger UI should be available at <http://localhost:9073/ds-present/api/>. 

The endpoint [record/{id}](http://localhost:9073/ds-present/api/#/ds-present/getRecord) should now
respond when requesting a record with the ID `local.test:luftfoto-sample.xml` (the GUI might be a little
slow to render the response).

## Test run with remote storage

Install [ds-storage](https://github.com/kb-dk/ds-storage) and follow its instructions for starting
a test instance.

Navigate to the `ds-present`-corpus folder and upload a sample to the running `ds-storage`:
```shell
cd src/test/resources/xml/corpus/
./post_to_storage.sh illum.xml

```
After this, a record for ID `id=doms.radio:illum.xml` should be delivered when requested from the
[record/{id}](http://localhost:9073/ds-present/api/#/ds-present/getRecord) endpoint.


## General setup

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
