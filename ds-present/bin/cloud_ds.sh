#!/bin/bash
set -e

#
# Digitale Samlinger specific script
# 
# Updates config for existing collection under alias ds-write to newest version
#

###############################################################################
# CONFIG
###############################################################################

: ${CONFIG_FOLDER:=""} # Should be set either by the caller or cloud.conf
: ${COMMAND:="$1"}
: ${COLLECTION_OR_ALIAS:="$2"} # Default is ds-write

if [[ -s "cloud.conf" ]]; then
    source "cloud.conf"     # Local overrides
fi
pushd ${BASH_SOURCE%/*} > /dev/null
if [[ -s "cloud.conf" ]]; then
    source "cloud.conf"     # Project overrides
fi
source general.conf
: ${LOG:="$(pwd)/cloud_commands.log"}
: ${CLOUD:="$(pwd)/cloud"}
# If true, existing configs with the same ID are overwritten
: ${FORCE_CONFIG:="false"}
: ${EXISTENCE_RETRIES:=5}
: ${RETRY_DELAY_SECONDS:=5}
popd > /dev/null
: ${COLLECTION_OR_ALIAS:="ds-write"}
: ${CONFIG_NAME:="ds-conf-$(grep 'name="_ds_.*_"' ${CONFIG_FOLDER}/schema.xml  | sed 's/.*name="_ds_\([0-9]\+\)\.\([0-9]\+\)\.\([0-9]\+\)_.*/\1.\2.\3/')"}

function usage() {
    cat <<EOF
Usage: ./cloud_update_config.sh new
Usage: ./cloud_update_config.sh update <collection|alias>
Usage: ./cloud_update_config.sh align

* new
Create a new collection and set the alias ds-write to point to that.
The collection will be named ds-YYYYmmdd-HHMM.

* update
Assign the latest Solr configuration to the collection.
If no collection is given, the collection pointed to by ds-write will
be used.

* align
Points the ds alias to the same collection as the ds-write alias.
Used after full reindex to make the searchers use the new index.
EOF
    exit $1
}

check_parameters() {
    if [[ -z "$COMMAND" ]]; then
        usage
    fi
    if [ "." == ".$CONFIG_FOLDER" -o "." == ".$CONFIG_NAME" ]; then
        usage
    fi
    if [ ! -d $CONFIG_FOLDER ]; then
        >&2 echo "The config folder '$CONFIG_FOLDER' does not exist"
        usage 2
    fi
    pushd "$CONFIG_FOLDER" > /dev/null
    CONFIG_FOLDER=$(pwd)
    popd > /dev/null
    if [ ! -s $CONFIG_FOLDER/schema.xml ]; then
        >&2 echo "No schema.xml in the config folder '$CONFIG_FOLDER'"
        usage 21
    fi
    if [ ! -d ${CLOUD}/$VERSION ]; then
        >&2 echo "No cloud present at ${CLOUD}/${VERSION}. Please install and start a cloud first with"
        >&2 echo "./cloud_install.sh $VERSION"
        >&2 echo "./cloud_start.sh $VERSION"
        exit 3
    fi

}

################################################################################
# FUNCTIONS
################################################################################

locate_solr_scripts() {
    : ${SOLR_SCRIPTS:="${CLOUD}/${VERSION}/solr1/server/scripts/cloud-scripts"}
    if [ ! -d $SOLR_SCRIPTS ]; then
        >&2 echo "Error: The Solr script folder '$SOLR_SCRIPTS' is not visible from `pwd`"
        exit 13
    fi
}

resolve_derived_settings() {
    # Resolve default
    : ${HOST:=`hostname`}
    : ${ZOO_BASE_PORT:=2181}
    : ${ZOOKEEPER:="$HOST:$ZOO_BASE_PORT"}
    
    : ${SOLR_BASE_PORT:=9000}
    : ${SOLR:="$HOST:$SOLR_BASE_PORT"}
    : ${SHARDS:=1}
    : ${REPLICAS:=1}
    
    : ${CONFIG_FOLDER:="config/solr/conf"}
    : ${PREFIX:= "ds"}
}

upload_config() {
    # Upload the config if it is not already in the cloud
    echo "Checking for existence of configuration $CONFIG_NAME in the Solr Cloud"
    set +e
    EXISTS="$($SOLR_SCRIPTS/zkcli.sh -zkhost $ZOOKEEPER -cmd list 2>&1 | grep -a /configs/$CONFIG_NAME/)" >> /dev/null 2>> /dev/null
    set -e
    if [[ "." == ".$EXISTS" || "true" == "$FORCE_CONFIG" ]]; then
        # Upload the config
        echo "Updating Solr config $CONFIG_NAME from $CONFIG_FOLDER to ZooKeeper at $ZOOKEEPER"
        echo "command> $SOLR_SCRIPTS/zkcli.sh -zkhost $ZOOKEEPER -cmd upconfig -confname $CONFIG_NAME -confdir \"$CONFIG_FOLDER\""
        $SOLR_SCRIPTS/zkcli.sh -zkhost $ZOOKEEPER -cmd upconfig -confname $CONFIG_NAME -confdir "$CONFIG_FOLDER" 2>&1 >> $LOG
    else
        >&2 echo "Solr config $CONFIG_NAME already exists. Keeping existing config."
        >&2 echo "Config update can be forced with FORCE_CONFIG=true"
    fi
}

create_alias() {
    local ALIAS="$1"
    local COLLECTION="$2"
    echo "Creating alias '$ALIAS' for collection '$COLLECTION'"
    local RESPONSE=$(curl -m 30 -s "http://$SOLR/solr/admin/collections?action=CREATEALIAS&name=${ALIAS}&collections=${COLLECTION}")
    if [[ "0" == $(jq .responseHeader.status <<< "$RESPONSE") ]]; then
        echo "Success"
    else
        >&2 echo "Failure:"
        >&2 echo "$RESPONSE"
        exit 50
    fi
}

create_new_collection() {
    echo "Creating new collection $COLLECTION"
    URL="http://$SOLR/solr/admin/collections?action=CREATE&name=${COLLECTION}&numShards=${SHARDS}&maxShardsPerNode=${SHARDS}&replicationFactor=${REPLICAS}&collection.configName=${CONFIG_NAME}"
    echo "request> $URL"
    RESPONSE="`curl -m 60 -s \"$URL\"`"
    if [ -z "$(grep 'status":0,' <<< "$RESPONSE")" ]; then
        >&2 echo "Failed to create collection ${COLLECTION} with config ${CONFIG_NAME}:"
        >&2 echo "$RESPONSE"
        exit 31
    fi

    echo "Setting alias 'ds-write' to collection $COLLECTION"
    create_alias ds-write $COLLECTION &> /dev/null
    
    echo "Collection with config '$CONFIG_NAME' available at http://$SOLR/solr/$COLLECTION"
    echo "Perform index, then set the 'ds' read-alias with"
    echo "bin/cloud_ds.sh align"
}

update_existing_collection() {
    echo "Collection $COLLECTION already exist. Assigning config $CONFIG_NAME"

    # ZooKeepers linkconfig does change part of the state in Solr 9, but does not result
    # in an effective config change
    #$SOLR_SCRIPTS/zkcli.sh -zkhost $ZOOKEEPER -cmd linkconfig -collection $COLLECTION -confname $CONFIG_NAME

    # Solr API v1 index config change should work, according to
    # https://solr.apache.org/guide/solr/latest/deployment-guide/collection-management.html#modifycollection
    # Calling this throws
    # java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class java.lang.String...
    # URL="http://$SOLR/solr/admin/collections?action=MODIFYCOLLECTION&collection=${COLLECTION}&collection.configName=${CONFIG_NAME}&collection.configName=${CONFIG_NAME}"
    # curl -s "$URL"

    # Solr API v2 index config change DOES work. At least for Solr 9.4
    local RESPONSE=$(curl -X POST "http://${SOLR}/api/collections/${COLLECTION}" -H 'Content-Type: application/json' \
                          -d '{"modify":{"config": "'$CONFIG_NAME'" } }')
    if [[ -z $(grep '"status":0' <<< "$RESPONSE") ]]; then
        >&2 echo "Error assigning config '$CONFIG_NAME' to collection '$COLLECTION'"
        >&2 echo ""
        >&2 echo "$REPONSE"
        exit 4
    fi
    
    echo "Reloading collection $COLLECTION"
    RESPONSE=`curl -m 120 -s "http://$SOLR/solr/admin/collections?action=RELOAD&name=$COLLECTION"`
    if [ -z "$(grep '"status":0,' <<< "$RESPONSE")" ]; then
        >&2 echo "Failed to reload collection ${COLLECTION}:"
        >&2 echo "$RESPONSE"
        exit 1
    fi
    echo "Collection with config '$CONFIG_NAME' available at http://$SOLR/solr/$COLLECTION"
}


# Input: Collection name
# Set EXISTS=<if collection exists>
check_existence() {
    local COL="$1"
    set +e
    COLLECTIONS="$(curl -m 30 -s "http://$SOLR/solr/admin/collections?action=LIST" | jq -r '.collections[]')"
    if [[ "." != ".$(grep "^$COL\$" <<< "$COLLECTIONS")" ]]; then
        EXISTS=true
    else
        EXISTS=false
    fi
    set -e
}

# Set COLLECTION=ds-YYYYmmdd-HHMM
new_collection_name() {
    COLLECTION="ds-$(date +%Y%m%d-%H%M)"
}

# Delete the collection given as first argument to the method.
delete_collection() {
    DELETE_URL="$SOLR/solr/admin/collections?action=DELETE&name=$1"
    echo "Calling $DELETE_URL"
    status_code=$(curl -s -o /dev/null -w "%{http_code}" "$DELETE_URL")

    if [ $status_code -eq 400 ]; then
        echo "Could not delete collection $1. There's probably one or more aliases pointing at it."
        return 1
    else
        return 0
    fi
}

# Solr can get a hard time, with to many collections. To handle this, the cloud_ds.sh script deletes old collections prefixed with ds.
cleanup_collections() {
    set +e
    # Filtering the result to only contain collections that are create with the cloud_ds.sh new command, which are all prefixed with 'ds'.
    ds_collections=$(curl -m 30 -s "http://$SOLR/solr/admin/collections?action=LIST" | jq --arg prefix $PREFIX '[.collections[] | select(startswith($prefix))]')
    # Count of ds collections
    collection_count=$(echo $ds_collections | jq '. | length')
    # Do clean up if more than 6 collections with prefix 'ds' exists.
    if [ $collection_count -gt 6 ]; then
        echo ""
        echo "More than 6 DS collections are available, deleting the oldest."
        counter=$collection_count
        # Sort collections to make sure oldest collections appear first in the array.
        sorted_collections=$(echo $ds_collections | jq '[sort | .[]]')
        entry_to_extract=0

        # We want to preserve the six newest collections.
        while [ $counter -gt 6 ]
        do
          # Get name of first collection in array
          current_collection=$(echo $sorted_collections | jq -r --argjson entry $entry_to_extract '.[$entry]')
          echo "Deleting collection: $current_collection"
          delete_collection $current_collection

          # If the delete method returned an error code only increment the entry iterator.
          if [ $? -eq 1 ]; then
              ((entry_to_extract ++))
          else
            # If no error code has been thrown, delete the entry from our array, increment the entry and decrement the loop iteration counter.
            sorted_collections=$(echo $sorted_collections | jq --argjson entry $entry_to_extract 'del(.[$entry])')
            ((entry_to_extract ++))
            ((counter--))
          fi

          echo ""
        done
    fi
    }

# Requires COLLECTION_OR_ALIAS
# Set COLLECTION=<resolved from alias or directly from COLLECTION_OR_ALIAS>
# Set EXISTS=<COLLECTION exists in the Solr Cloud>
resolve_collection() {
    EXISTS=false
    
    set +e
    COLLECTIONS="$(curl -m 30 -s "http://$SOLR/solr/admin/collections?action=LIST" | jq -r '.collections[]')"
    if [[ "." != ".$(grep "^$COLLECTION_OR_ALIAS\$" <<< "$COLLECTIONS")" ]]; then
        COLLECTION="$(grep "^$COLLECTION_OR_ALIAS\$" <<< "$COLLECTIONS")"
        echo "Existing collection given directly as '$COLLECTION'"
        EXISTS=true
    else
        # Check if input was an alias
        COLLECTION=$(curl -m 30 -s "http://$SOLR/solr/admin/collections?action=LISTALIASES" | jq -r ".aliases.\"$COLLECTION_OR_ALIAS\"")
        if [[ "null" == "$COLLECTION" ]]; then
            echo "Unable to locate a collection directly or for alias '$COLLECTION_OR_ALIAS'"
        else
            echo "Resolved alias '$COLLECTION_OR_ALIAS' to collection '$COLLECTION'"
            EXISTS=true
        fi
    fi
    set -e
}

align_ds_ds_write() {
    echo "Setting alias ds to collection $COLLECTION"
    create_alias ds $COLLECTION
    echo "Alias 'ds' and alias 'ds-write' now points to collection '$COLLECTION'"
    echo "Available at http://$SOLR/solr/ds"
}
    

###############################################################################
# CODE
###############################################################################

check_parameters "$@"

locate_solr_scripts
resolve_derived_settings

pushd ${CLOUD}/$VERSION > /dev/null
#upload_config
popd > /dev/null

if [[ -z "$COLLECTION_OR_ALIAS" ]]; then
    >&2 echo "Error: No alias or collection to create or update. Exiting"
    usage
    exit
fi

case $COMMAND in
    \-h)
        usage
        exit
        ;;
    new)
        new_collection_name
        check_existence $COLLECTION
        if [[ "true" == "$EXISTS" ]]; then
            >&2 echo "Error: Uable to create new collection '$COLLECTION' as it already exists"
            >&2 echo "Note: Only 1 collection can be created per minute."
            exit 51
        fi
        upload_config
        create_new_collection
        exit
        ;;
    update)
        # Check to see if the collection is already there
        resolve_collection
        if [[ "false" == "$EXISTS" ]]; then
            >&2 echo "Error: Unable to update config for '$COLLECTION' as it cannot be resolved"
            exit 52
        fi
        upload_config
        update_existing_collection
        exit
        ;;
    align)
        COLLECTION_OR_ALIAS="ds-write"
        resolve_collection
        if [[ "false" == "$EXISTS" ]]; then
            >&2 echo "Error: Unable to resolve alias ds-write"
            exit 53
        fi
        align_ds_ds_write
        # Cleanup after aligning
        cleanup_collections
        exit
        ;;
    *)
        >&2 echo "Unknown command '$COMMAND'"
        >&2 echo ""
        usage
        exit 53
        ;;
esac
