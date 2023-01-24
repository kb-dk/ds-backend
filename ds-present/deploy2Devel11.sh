#!/bin/sh

mvn clean package -DskipTests
mv target/ds-present*.war target/ds-present.war

scp target/ds-present.war digisam@devel11:/home/digisam/services/tomcat-apps/

echo "ds-present deployed to devel11"
