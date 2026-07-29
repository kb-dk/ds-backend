#!/bin/bash

{
    echo "Waiting for Solr to boot up..."

    until curl -s "http://localhost:8983/solr/admin/info/system" > /dev/null; do
        sleep 2
    done

    echo "Solr is up and running!"
    #Try
    #/opt/solr/bin/solr create_collection -c repository -d /opt/solr/user_config/conf -n sw_conf1 -shards 1
    /opt/solr/bin/solr zk upconfig -n my_config -d /opt/solr/user_config/conf -z localhost:9983

    if curl -s "http://localhost:8983/solr/admin/collections?action=LIST" | grep -q '"ds-collection"'; then
        echo "Collection 'ds-collection' already exists."
    else
        echo "Creating collection 'ds-collection' using 'my_config'..."
        curl -s "http://localhost:8983/solr/admin/collections?action=CREATE&name=ds-collection&numShards=1&replicationFactor=1&collection.configName=my_config"
    fi

    #Try
    #/opt/solr/bin/solr api -get "http://localhost:8983/solr/admin/collections?action=CREATEALIAS&name=my_alias&collections=repository"
    echo "Pointing alias 'ds-write' to collection 'ds-collection'..."
    curl -s "http://localhost:8983/solr/admin/collections?action=CREATEALIAS&name=ds-write&collections=ds-collection"

    echo "Solr is completely good to go!"
} &