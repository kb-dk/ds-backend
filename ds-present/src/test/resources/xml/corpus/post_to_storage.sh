#!/bin/bash

# Requires a running instance of https://github.com/kb-dk/ds-storage/
# with default test setup

: ${FILE:="$1"}
: ${FILE:="illum.xml"}

: ${STORAGE:="http://localhost:8080/ds-storage/v1"}

ID="doms.radio:$FILE"

T=$(mktemp)
echo '{"id":"'$ID'", "base":"doms.radio", "data":'$(jq -R -s '.' < $FILE)'}' > $T
curl -s -X POST "$STORAGE/createOrUpdateRecord" -H  "accept: */*" -H  "Content-Type: application/json" -d @${T}
rm "$T"

echo "Indexed $ID to ds-storage."
echo "Access at ${STORAGE}/getRecord?id=$ID"
