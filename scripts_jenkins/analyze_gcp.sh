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
SONARQUBE_URL_DOMAIN="https://sonarqube-jenkins-test.bmg.com"
BACKEND_PROJECT="bmg-supplychain-deliver_upgrade-backend"
BACKEND_KEY="bmg-supplychain-deliver_upgrade-backend-test"

if [ $STAGE = "test" ]
  then
    PROXY="10.14.232.48"
elif [ $STAGE = "staging" ]
  then
    PROXY="squid.k8s.bmg-monitoring-prod.gcp.internal.bmg.com"
    STAGE="stage"
  else
    PROXY="10.14.232.183"
fi
export http_proxy="http://${PROXY}:3128"
export https_proxy="http://${PROXY}:3128"
export no_proxy="localhost,10.14.234.74,https://nexus.bmg.com,*.gcr.io,gcr.io,metadata.google.internal,registry-1.docker.io"

echo -n "systemProp.https.proxyHost=${PROXY}
systemProp.https.proxyPort=3128
systemProp.http.proxyHost=${PROXY}
systemProp.http.proxyPort=3128
systemProp.https.nonProxyHosts=localhost,https://nexus.bmg.com,*.bmg.com,*.gcr.io" >> gradle.properties

echo """
{
  \"auths\": {},
	\"credHelpers\": {
		\"asia.gcr.io\": \"gcr\",
		\"eu.gcr.io\": \"gcr\",
		\"gcr.io\": \"gcr\",
		\"marketplace.gcr.io\": \"gcr\",
		\"us.gcr.io\": \"gcr\",
    \"europe-west1-docker.pkg.dev\": \"gcr\"
	},
  \"proxies\": {
    \"default\": {
      \"httpProxy\": \"${http_proxy}\",
      \"httpsProxy\": \"${https_proxy}\",
      \"noProxy\": \"${no_proxy}\"
    }
  }
} """ > config.json
mkdir -p ~/.docker
cp config.json ~/.docker/
rm config.json

echo """
[Service]
Environment="HTTP_PROXY=${http_proxy}"
Environment="HTTPS_PROXY=${https_proxy}"
Environment="NO_PROXY=localhost,10.14.234.74,https://nexus.bmg.com,*.bmg.com,*.gcr.io,gcr.io"
""" > proxy.conf
sudo mv proxy.conf /etc/systemd/system/docker.service.d/
cat /etc/systemd/system/docker.service.d/proxy.conf

./gradlew -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=3128 -Dhttps.proxyHost=${PROXY} -Dhttps.proxyPort=3128 -Dhttp.nonProxyHosts=10.14.234.74 clean assemble
./gradlew -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY} -Dhttps.proxyPort=${PROXY_PORT} -Dhttp.nonProxyHosts=10.14.234.74 test
./gradlew -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY} -Dhttps.proxyPort=${PROXY_PORT} -Dhttp.nonProxyHosts=10.14.234.74 -Dsonar.verbose=true -Dsonar.host.url=${SONARQUBE_URL_DOMAIN} -Dsonar.projectName=${BACKEND_PROJECT} -Dsonar.projectKey=${BACKEND_KEY} -Dsonar.login=${2} sonar