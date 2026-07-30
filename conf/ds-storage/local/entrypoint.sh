#!/bin/sh
# Use 'tr' to ensure no weird characters snuck in, though envsubst is usually clean
envsubst < /usr/local/tomcat/conf/ds-storage-configs/ds-storage-devel.template.yaml > /usr/local/tomcat/conf/ds-storage-configs/ds-storage-devel.yaml
rm /usr/local/tomcat/conf/ds-storage-configs/ds-storage-devel.template.yaml
chmod 644 /usr/local/tomcat/conf/ds-storage-configs/ds-storage-devel.yaml
# Start Tomcat
exec "$@"