# ds-present

Transforming access layer for metadata. Acts as a proxy for [ds-storage](https://github.com/kb-dk/ds-storage) 
providing multiple views on metadata, such as MODS, JSON-LD and SolrJsonDocument.

Developed and maintained by the Royal Danish Library.

## ⚠️ Warning: Copyright Notice
Please note that it is not permitted to download and/or otherwise reuse content from the DR-archive at The Danish Royal Library.


## Requirements

* Maven 3                                  
* Java 11
* For production or integration test: A running [ds-storage](https://github.com/kb-dk/ds-storage)

## Test run

Build and start the service using jetty with
```shell
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

### Solr fundamentals

In DS, Solr operates with versioned collections. Upon first start or if a full reindex is needed, a timestamped collection is created. Access to the collection is done through two aliases:

* **ds** is used for searching
* **ds-write** is used for updating

Normally the two aliases will point to the same timestamped collection, but if a full index rebuild is underway, **ds** will point to the complete old collection while **ds-write** will point to the new collection being created. When the full index rebuild has finished, **ds** will be switched to the new collection.

This method ensures that there are no disruptions to Solr search. It is used in production at the Royal Danish Library and also on devel + stage for consistency.

The how-to below expects the version of the Solr configuration to be specified in `schema.xml` as a special field looking like this:
```xml
 <field name="_ds_1.0.0_" type="string" indexed="false" stored="false"/>
```
If the schema is changed, the version must be updated using [Semantic Versioning](https://semver.org/)

### Initial setup of Solr Cloud

ds-present comes with convenience scripts for downloading, installing and starting Solr 9 in Cloud mode.

```shell
  bin/cloud_install.sh
  bin/cloud_start.sh
 
  bin/cloud_ds.sh new
  bin/cloud_ds.sh align
```

Check that the collection was created by visiting
[http://localhost:10007/solr/#/~cloud?view=graph](http://localhost:10007/solr/#/~cloud?view=graph)

After this Solr is available at http://localhost:10007/solr/ 

### Operating Solr

The cloud can be stopped and started with
```shell
bin/cloud_stop.sh
bin/cloud_start.sh
```

Aliases and collections can be seen from the admin gui or by calling
```shell
bin/cloud_alias.sh
```
and
```shell
bin/cloud_status.sh
```

Clear all records in the `ds-20231114-1446` collection, but keep the collection:
```shell
bin/cloud_clear.sh ds-20231114-1446
```

Fully delete a concrete collection, e.g. `ds-20231114-1446`:
```shell
bin/cloud_delete.sh ds-20231114-1446
```

### Configuration update without reindex


If the Solr configuration is changed in a way that does not require a full reindex, the
configuration can be assigned to an existing collection.

The existing collection should be the one that the alias `ds-write` points to.
In that case the updated configuration can be assigned with
```shell
bin/cloud_ds.sh update
```
If not, the aliases and collections can be listed with `bin/cloud_alias.sh` and the 
relevant collection can be fiven as argument
```shell
bin/cloud_ds.sh update <a-specific-collection>
```

If this is during development and the Solr config version has not been bumped, the update can be forced:
```shell
FORCE_CONFIG=true bin/cloud_ds.sh update
```


### Full reindex

If a fresh index is needed, the alias mehanism ensures that this is done without disrupting
the existing collection.

Create a new empty index with
```shell
bin/cloud_ds.sh new
```
This will create a new index and set the `ds-write` alias to point to it. It can be inspected with
```shell
bin/cloud_alias.sh
```
which should give something like

```
Aliases:
{
  "ds-write": "ds-20231116-1240",
  "ds": "ds-20231116-1045"
}

Collections:
ds-20231116-1045
ds-20231116-1051
ds-20231116-1240
```
Note how `ds-write` and `ds` points to different collections.


Perform the full reindex (`http://<develserver>/ds-datahandler/api/#/ds-datahandler/solrIndex`).
Users of the index (`ds-license` and `ds-discover`) will continue to query the old index
while this takes place.

When the index has finished, adjust the **ds** alias to point to the new full collection
```shell
bin/cloud_ds.sh align
```

Users of the index (`ds-license` and `ds-discover`) will now use the new index.

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
```shell 
mvn package
```

Test the webservice with
```shell
mvn jetty:run
```

The Swagger UI is available at <http://localhost:9073/ds-present/api/>. 

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
    <groupId>dk.kb.present</groupId>
    <artifactId>ds-present</artifactId>
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
    DsPresentClient presentClient = new DsPresentClient("https://example.com/ds-present/v1");
```
During development, a SNAPSHOT for the OpenAPI client can be installed locally by running
```shell
mvn install
```


See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
