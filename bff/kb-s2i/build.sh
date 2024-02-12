#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/bff-*.war "$TOMCAT_APPS/bff.war"
cp -- /tmp/src/conf/ocp/bff.xml "$TOMCAT_APPS/bff.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/bff.war")
