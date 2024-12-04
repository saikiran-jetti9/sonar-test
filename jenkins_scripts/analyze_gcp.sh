#!/usr/bin/env sh
set -x
set -e

if [ -n "$1" ]
  then
    STAGE="$1"
  else
    echo "STAGE must be specified" && exit 1
fi
token="$2"
PROXY_PORT="3128"
SONARQUBE_URL_DOMAIN="http://sonarqube-jenkins-test.bmg.com"
BACKEND_PROJECT="bmg-trigon-masterdata-test"
APP_ENV=$STAGE
if [ $STAGE = "test" ]
  then
    PROXY="10.14.232.48"
elif [ $STAGE = "staging" ]
  then
    PROXY="squid.k8s.bmg-monitoring-prod.gcp.internal.bmg.com"
    STAGE="stage"
    APP_ENV="uat"
  else
    PROXY="10.14.232.183"
fi
export http_proxy="http://${PROXY}:3128"
export https_proxy="http://${PROXY}:3128"
export no_proxy="localhost,https://nexus.bmg.com,*.gcr.io,gcr.io,metadata.google.internal,registry-1.docker.io"

echo -n "systemProp.https.proxyHost=${PROXY}
systemProp.https.proxyPort=3128
systemProp.http.proxyHost=${PROXY}
systemProp.http.proxyPort=3128
systemProp.https.nonProxyHosts=localhost,https://nexus.bmg.com,*.bmg.com,*.gcr.io" >> gradle.properties

echo """
[Service]
Environment="HTTP_PROXY=${http_proxy}"
Environment="HTTPS_PROXY=${https_proxy}"
Environment="NO_PROXY=localhost,https://nexus.bmg.com,*.bmg.com,*.gcr.io,gcr.io"
""" > proxy.conf
sudo mv proxy.conf /etc/systemd/system/docker.service.d/
cat /etc/systemd/system/docker.service.d/proxy.conf

update_property() {
  local prop_name="$1"
  local new_value="$2"
  sed -i "s/property \"$prop_name\",\s*\"[^\"]*\"/property \"$prop_name\",\ \"$new_value\"/g" build.gradle
}
# Update properties in build.gradle
update_property "sonar.projectName" "${BACKEND_PROJECT}"
update_property "sonar.projectKey" "${BACKEND_PROJECT}"
update_property "sonar.host.url" "HOSTURL"
sed -i 's/HOSTURL/http:\/\/sonarqube-jenkins-test.bmg.com/g' build.gradle
# comment out password sonar property
login_line_number=$(grep -n "property 'sonar.login'" build.gradle | cut -d ":" -f1)
sed -i "${login_line_number}s/^/\/\/ /" build.gradle
password_line_number=$(grep -n "property 'sonar.password'" build.gradle | cut -d ":" -f1)
sed -i "${password_line_number}s/^/\/\/ /" build.gradle

./gradlew -Dhttp.proxyHost=10.14.232.183 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=10.14.232.183 -Dhttps.proxyPort=3128 -Dhttp.nonProxyHosts=10.14.234.74 clean assemble -PappEnvironment=${APP_ENV}
./gradlew -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY} -Dhttps.proxyPort=${PROXY_PORT} -Dhttp.nonProxyHosts=10.14.234.74 -Dsonar.host.url=${SONARQUBE_URL_DOMAIN} -Dsonar.projectName=${BACKEND_PROJECT} -Dsonar.projectKey=${BACKEND_PROJECT} -Dsonar.login=${2} sonar -PappEnvironment=${APP_ENV}