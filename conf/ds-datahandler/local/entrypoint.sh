#!/bin/sh
# Use 'tr' to ensure no weird characters snuck in, though envsubst is usually clean
envsubst < /usr/local/tomcat/conf/ds-datahandler-configs/ds-datahandler-devel.template.yaml > /usr/local/tomcat/conf/ds-datahandler-configs/ds-datahandler-devel.yaml
rm /usr/local/tomcat/conf/ds-datahandler-configs/ds-datahandler-devel.template.yaml
chmod 644 /usr/local/tomcat/conf/ds-datahandler-configs/ds-datahandler-devel.yaml
# Start Tomcat
exec "$@"