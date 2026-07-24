#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/ds-license-*.war "$TOMCAT_APPS/ds-license.war"
cp -- /tmp/src/conf/ocp/ds-license.xml "$TOMCAT_APPS/ds-license.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/ds-license.war")
