#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-present-*.war "$TOMCAT_APPS/ds-present.war"
cp -- /tmp/src/conf/ocp/ds-present.xml "$TOMCAT_APPS/ds-present.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-present.war")
