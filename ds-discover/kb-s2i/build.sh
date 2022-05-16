#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-discover-*.war "$TOMCAT_APPS/ds-discover.war"
cp -- /tmp/src/conf/ocp/ds-discover.xml "$TOMCAT_APPS/ds-discover.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-discover.war")
