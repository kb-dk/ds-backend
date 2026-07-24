#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-image-*.war "$TOMCAT_APPS/ds-image.war"
cp -- /tmp/src/conf/ocp/ds-image.xml "$TOMCAT_APPS/ds-image.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-image.war")
