#!/bin/bash

: ${INPUT:="$1"}
: ${INPUT:="resources/xml/copyright_extraction/000332.tif.xml"}

: ${USE_ID:="$2"}
: ${USE_ID:="ds.samlinger:05fea810-7181-11e0-82d7-002185371280"}

: ${XSLT:="../main/resources/xslt/mods2solr.xsl"}

: ${SAXON_JAR:="/home/$USER/saxon/saxon-he-11.4.jar"}
SAXON="java -jar "$SAXON_JAR" --suppressXsltNamespaceCheck:on  "
if [[ ! -s "$SAXON_JAR" ]]; then
    >&2 echo "$SAXON_JAR not available. Please install it from https://sourceforge.net/projects/saxon/files/Saxon-HE/"
    >&2 echo "The installation folder should be /home/$USER/saxon/"
    exit 2
fi
for TOOL in jq; do
    if [[ -z $(which $TOOL) ]]; then
        >&2 echo "$TOOL not available. Please install it"
        exit 3
    fi
done
if [[ ! -s "$INPUT" ]]; then
    >&2 echo "Input file not available: '$INPUT'"
    echo ""
    echo "Usage: ./transform.sh <inputfile>"
    exit 2
fi
if [[ ! -s "$XSLT" ]]; then
    >&2 echo "No XSLT '$XSLT'"
    echo ""
    echo "Usage: ./transform.sh <inputfile>"
    exit 2
fi
use_id=''
if [ $USE_ID ]; then
    use_id="record_identifier=$USE_ID"
fi

# set to 1 if you want to prettyprint you json

#json=$(sed 's/.xml$/.json/' <<< "$file")
$SAXON -xsl:"$XSLT" -s:"$INPUT" $use_id | jq .
