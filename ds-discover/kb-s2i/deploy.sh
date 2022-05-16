#!/usr/bin/env bash

# TODO: Should this be ds-discover-logback.xml?
cp -- /tmp/src/conf/ocp/logback.xml "$CONF_DIR/logback.xml"
# There are normally two configurations: core and environment
cp -- /tmp/src/conf/ds-discover-*.yaml "$CONF_DIR/"
 
ln -s -- "$TOMCAT_APPS/ds-discover.xml" "$DEPLOYMENT_DESC_DIR/ds-discover.xml"
