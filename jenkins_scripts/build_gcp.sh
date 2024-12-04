#!/usr/bin/env sh
set -x
set -e

if [ -n "$1" ]
  then
    STAGE="$1"
  else
    echo "STAGE must be specified" && exit 1
fi

#Application info
APP_NAME="trigon-masterdata"
PROJECT_COMMON_NAME="bmg-sap-royalty-post"
GCP_REGISTRY="europe-west1-docker.pkg.dev"
GCP_PROJECT="${PROJECT_COMMON_NAME}-${STAGE}"
IMAGE="${GCP_REGISTRY}/${GCP_PROJECT}/trigon/masterdata"
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
    PROXY="10.14.232.48"  
    # PROXY="10.14.232.183"
fi

if [ -z "$VERSION" ] ; then
  VERSION=$BUILD_NUMBER
fi
#export proxy
export http_proxy="http://${PROXY}:3128"
export https_proxy="http://${PROXY}:3128"
export no_proxy="https://nexus.bmg.com"

export PATH="$PATH:/opt/gradle-8.1.1/bin"

echo -n "systemProp.https.proxyHost=${PROXY}
systemProp.https.proxyPort=3128
systemProp.http.proxyHost=${PROXY}
systemProp.http.proxyPort=3128
systemProp.https.nonProxyHosts=localhost,https://nexus.bmg.com,*.bmg.com,europe-west1-docker.pkg.dev,*.gcr.io" >> gradle.properties

#Build and test application
./gradlew clean assemble -PappEnvironment=${APP_ENV}

#Executing test cases
echo "Run ./gradlew test "
# ./gradlew test -PappEnvironment=${APP_ENV}

#Proxy configuration for docker
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

#Docker creds configure
echo "Docker configure"
wget "https://github.com/GoogleCloudPlatform/docker-credential-gcr/releases/download/v2.0.0/docker-credential-gcr_linux_amd64-2.0.0.tar.gz"
tar -xzvf docker-credential-gcr_linux_amd64-2.0.0.tar.gz
sudo cp docker-credential-gcr /usr/local/bin/docker-credential-gcr
sudo chmod +x /usr/local/bin/docker-credential-gcr;
unset http_proxy https_proxy
docker-credential-gcr configure-docker

#docker build and push
echo 'Running build 3 (build and push image)'
./gradlew -Djib.useOnlyProjectCache=true jib -PappEnvironment=${APP_ENV} --image=${IMAGE}:${VERSION}