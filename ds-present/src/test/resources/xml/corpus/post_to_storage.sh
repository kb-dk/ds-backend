#!/bin/bash

# Requires a running instance of https://github.com/kb-dk/ds-storage/
# with default test setup

: ${RECORD_FILES:="$@"}
: ${RECORD_FILES:="albert-einstein.xml hvidovre-teater.xml simonsen-brandes.xml tystrup-soroe.xml homiliae-super-psalmos.xml work_on_logic.xml joergen_hansens_visebog.xml responsa.xml"}

: ${STORAGE:="http://localhost:9072/ds-storage/v1"}
: ${ENDPOINT:="$STORAGE/record"}

post_record() {
    local RECORD_FILE="$1"

    ID="doms.radio:$RECORD_FILE"

    T=$(mktemp)
    echo '{"id":"'$ID'", "base":"doms.radio", "data":'$(jq -R -s '.' < $RECORD_FILE)'}' > $T

    STATUSCODE=$(curl -s --output /dev/stderr --write-out "%{http_code}" -X POST "$ENDPOINT" -H  "accept: */*" -H  "Content-Type: application/json" -d @${T})

    if [ ! "$STATUSCODE" -eq 204 ]; then
        >&2 echo "Error: Unable to post content to ds-storage"
        >&2 echo "Got HTTP code $STATUSCODE for POST to $ENDPOINT"
        >&2 echo "Check if storage is running at ${STORAGE}"
        exit 2
    fi

    #curl -s -X POST "$STORAGE/record/createOrUpdateRecord" -H  "accept: */*" -H  "Content-Type: application/json" -d @${T}
    rm "$T"

    echo "Indexed ${ID}. Access at ${STORAGE}/record/$ID"
}

for RECORD_FILE in $RECORD_FILES; do
    post_record "$RECORD_FILE"
done

echo "All done."
