# ds-present

Transforming access layer for metadata. Acts as a proxy for [ds-storage](https://github.com/kb-dk/ds-storage) 
providing multiple views on metadata, such as MODS, JSON-LD and SolrJsonDocument.

Developed and maintained by the Royal Danish Library.


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

Navigate to the `ds-present`-corpus folder and upload samples to the running `ds-storage`:
```shell
cd src/test/resources/xml/corpus/
./post_to_storage.sh albert-einstein.xml hvidovre-teater.xml simonsen-brandes.xml tystrup-soroe.xml homiliae-super-psalmos.xml work_on_logic.xml joergen_hansens_visebog.xml responsa.xml
```
After this, a record for ID `doms.radio:albert-einstein.xml` should be delivered when requested from the
[/record/{id}](http://localhost:9073/ds-present/api/#/ds-present/record) endpoint.


## Test with Solr

### Setup Solr

Checkout [ds-solr](https://github.com/kb-dk/ds-solr/), change to the `solr-nested-indexing` branch and follow
the README, using the Solr setup from `nested-template/conf/`. This boils down to
```shell
  bin/cloud_install.sh
  bin/cloud_start.sh
  bin/cloud_sync.sh src/test/resources/solr/dssolr/conf/ ds-conf ds
```

Check that the collection was created by visiting
[http://localhost:10007/solr/#/~cloud?view=graph](http://localhost:10007/solr/#/~cloud?view=graph)

After this Solr is available at http://localhost:10007/solr/ and can be stopped and started with
```
bin/cloud_stop.sh
bin/cloud_start.sh
```

If the Solr configuration is changed, force an update with
```
FORCE_CONFIG=true bin/cloud_sync.sh src/test/resources/solr/dssolr/conf/ ds-conf ds
```

Clear the collection with 
```
bin/cloud_delete.sh ds
```


### Extract SolrJSONDocuments and index in Solr

With `ds-storage` populated with the sample documents and `ds-present` running, use the 
[ds-present API](http://localhost:9073/ds-present/api/) to call 
[/records](http://localhost:9073/ds-present/api/#/ds-present/records) for collection `remote`
and send them to Solr. 

This can be done from the command line with
```shell
curl -s 'http://localhost:9073/ds-present/v1/records?collection=remote&maxRecords=1000&format=SolrJSON' > solrdocs.json

Indekser dem i Solr:

curl -X POST -H 'Content-Type: application/json' 'http://localhost:10007/solr/ds/update' --data-binary @solrdocs.json
curl -X POST -H 'Content-Type: application/json' 'http://localhost:10007/solr/ds/update' --data-binary '{ "commit": {} }'
```

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
