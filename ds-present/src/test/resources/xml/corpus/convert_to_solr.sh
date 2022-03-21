#!/bin/bash

SOURCE="./"
TRANSFORM="../../../../main/resources/xslt/mods2solr.xsl"

: ${SAXON_JAR:="/home/$USER/saxon/saxon9he.jar"}
if [[ ! -s "$SAXON_JAR" ]]; then
    >&2 echo "$SAXON_JAR not available. Please install it from https://sourceforge.net/projects/saxon/files/Saxon-HE/"
    exit 2
fi
for TOOL in jq; do
    if [[ -z $(which $TOOL) ]]; then
        >&2 echo "$TOOL not available. Please install it"
        exit 3
    fi
done

SAXON="java -jar "$SAXON_JAR" --suppressXsltNamespaceCheck:on  "

# set to 1 if you want to prettyprint you json

DEBUG_JSON=1

mods_files=("albert-einstein.xml"
	    "hvidovre-teater.xml"
	    "simonsen-brandes.xml"
	    "tystrup-soroe.xml"
	    "homiliae-super-psalmos.xml"
	    "work_on_logic.xml"
	    "joergen_hansens_visebog.xml"
	    "responsa.xml")

for file in ${mods_files[@]}; do
    json=$(sed 's/.xml$/.json/' <<< "$file")
    echo " - Producing $json"
    if [ "$DEBUG_JSON" = "1" ]; then
	$SAXON -xsl:"$TRANSFORM" -s:"$SOURCE/$file" | jq . > "$json"
    else
	$SAXON -xsl:"$TRANSFORM" -s:"$SOURCE/$file" > "$json"
    fi
done
