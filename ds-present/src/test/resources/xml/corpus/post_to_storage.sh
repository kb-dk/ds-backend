#!/bin/bash

# Requires a running instance of https://github.com/kb-dk/ds-storage/
# with default test setup

: ${FILE:="$1"}
: ${FILE:="illum.xml"}

: ${STORAGE:="http://localhost:8080/ds-storage/v1"}
: ${ENDPOINT:="$STORAGE/record"}

ID="doms.radio:$FILE"

T=$(mktemp)
echo '{"id":"'$ID'", "base":"doms.radio", "data":'$(jq -R -s '.' < $FILE)'}' > $T

STATUSCODE=$(curl -s --output /dev/stderr --write-out "%{http_code}" -X POST "$ENDPOINT" -H  "accept: */*" -H  "Content-Type: application/json" -d @${T})

if [ ! "$STATUSCODE" -eq 204 ]; then
    >&2 echo "Error: Unable to post content to ds-storage"
    >&2 echo "Got HTTP code $STATUSCODE for POST to $ENDPOINT"
    >&2 echo "Check if storage is running at ${STORAGE}"
    exit 2
fi

#curl -s -X POST "$STORAGE/record/createOrUpdateRecord" -H  "accept: */*" -H  "Content-Type: application/json" -d @${T}
rm "$T"

echo "Indexed $ID to ds-storage."
echo "Access at ${STORAGE}/record/$ID"
