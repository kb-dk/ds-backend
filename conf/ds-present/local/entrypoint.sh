#!/bin/sh
# Use 'tr' to ensure no weird characters snuck in, though envsubst is usually clean
envsubst < /usr/local/tomcat/conf/ds-present-configs/ds-present-devel.template.yaml > /usr/local/tomcat/conf/ds-present-configs/ds-present-devel.yaml
rm /usr/local/tomcat/conf/ds-present-configs/ds-present-devel.template.yaml
chmod 644 /usr/local/tomcat/conf/ds-present-configs/ds-present-devel.yaml
# Start Tomcat
exec "$@"