#!/bin/sh
# Use 'tr' to ensure no weird characters snuck in, though envsubst is usually clean
envsubst < /usr/local/tomcat/conf/bff-configs/bff-devel.template.yaml > /usr/local/tomcat/conf/bff-configs/bff-devel.yaml
rm /usr/local/tomcat/conf/bff-configs/bff-devel.template.yaml
chmod 644 /usr/local/tomcat/conf/bff-configs/bff-devel.yaml
# Start Tomcat
exec "$@"
