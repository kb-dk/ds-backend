#!/bin/sh

mvn clean package -DskipTests
mv target/ds-license*.war target/ds-license.war

scp target/ds-license.war digisam@devel11:/home/digisam/services/tomcat-apps/

echo "ds-license deployed to devel11"
