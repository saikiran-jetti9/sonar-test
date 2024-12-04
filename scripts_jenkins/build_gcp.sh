#!/usr/bin/env sh
set -x
set -e

if [ -n "$1" ]
  then
    STAGE="$1"
  else
    echo "STAGE must be specified" && exit 1
fi

APP_NAME="deliver"
PROJECT_COMMON_NAME="bmg-supplychain"
GCP_REGISTRY="europe-west1-docker.pkg.dev"
GCP_PROJECT="${PROJECT_COMMON_NAME}-${STAGE}"
IMAGE="${GCP_REGISTRY}/${GCP_PROJECT}/${APP_NAME}/backend"
TAG="Jenkins-${BUILD_NUMBER}"

if [ $STAGE = "test" ]
  then
    PROXY="10.14.232.48"
elif [ $STAGE = "staging" ]
  then
    # PROXY="squid.k8s.bmg-monitoring-prod.gcp.internal.bmg.com"
    PROXY="10.14.232.48"
    STAGE="stage"
  else
    PROXY="10.14.232.183"
fi
export http_proxy="http://${PROXY}:3128"
export https_proxy="http://${PROXY}:3128"
export no_proxy="https://nexus.bmg.com,*.gcr.io,gcr.io,metadata.google.internal,registry-1.docker.io"

GRADLE_OPTIONS="-Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=3128 -Dhttps.proxyHost=${PROXY} \
-Dhttps.proxyPort=3128 -Dhttp.nonProxyHosts='*.gcr.io,https://nexus.bmg.com'"

echo -n "systemProp.https.proxyHost=${PROXY}
systemProp.https.proxyPort=3128
systemProp.http.proxyHost=${PROXY}
systemProp.http.proxyPort=3128
systemProp.https.nonProxyHosts=localhost,https://nexus.bmg.com,*.bmg.com,*.gcr.io" >> gradle.properties

#Build  application
./gradlew clean assemble

#Executing test cases
echo "Run ./gradlew test "
./gradlew test

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

echo "Docker credentials configure"
wget "https://github.com/GoogleCloudPlatform/docker-credential-gcr/releases/download/v2.0.0/docker-credential-gcr_linux_amd64-2.0.0.tar.gz"
tar -xzvf docker-credential-gcr_linux_amd64-2.0.0.tar.gz
sudo cp docker-credential-gcr /usr/local/bin/docker-credential-gcr
sudo chmod +x /usr/local/bin/docker-credential-gcr;
unset http_proxy https_proxy
docker-credential-gcr configure-docker

echo "Push docker image"
echo "${IMAGE}:Jenkins-${TAG}"
./gradlew jib --image=${IMAGE}:${TAG}