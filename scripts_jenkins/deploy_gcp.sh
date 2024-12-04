#!/usr/bin/env sh

set -x

GitAppRepo="bitbucket.org/bmgpipeline/deliver-mt-gke.git"
GitAppRepoName="deliver-mt-gke"
GitAppBranch="main"
guser="$2"
gpass="$3"
duser="$4"

if [ -n "$1" ]
  then
    STAGE="$1"
  else
    echo "STAGE must be specified" && exit 1
fi
GCP_PROJECT="bmg-supplychain-test"
if [ ${STAGE} = "staging" ]
  then
    STAGE="stage"
fi


TAG="Jenkins-${BUILD_ID}"
rm -rf $GitAppRepoName || true

git clone https://${guser}:${gpass}@${GitAppRepo}
cd ${GitAppRepoName}

# email="${duser:1:-1}"
email=$(echo "$duser" | sed 's/^.//;s/.$//')
name="${email%%@*}"

git config --global user.email $email
git config --global user.name  $name

echo -n ${TAG} >overlays/${STAGE}/versions/backend.version
git add overlays/${STAGE}/versions/backend.version
git commit -m "${STAGE} Deliver-backend deployed with tag ${TAG}, from deploy pipeline"
git push origin $GitAppBranch
