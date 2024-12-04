#!/usr/bin/env sh

set -x

GitAppRepo="bitbucket.org/bmgpipeline/trigon-mt-gke.git"
GitAppRepoName="trigon-mt-gke"
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

if [ ${STAGE} = "staging" ]
  then
    STAGE="stage"
fi


TAG="${VERSION}"
rm -rf $GitAppRepoName || true

#Clone MT repository
git clone https://${guser}:${gpass}@${GitAppRepo}
cd ${GitAppRepoName}

#Format triggered user info
email=$(echo "$duser" | sed 's/^.//;s/.$//')
name="${email%%@*}"

#Git config user
git config --global user.email $email
git config --global user.name  $name

#Update backend version/tag
echo -n ${TAG} >overlays/${STAGE}/versions/masterdata.version

#Git update
git add overlays/${STAGE}/versions/masterdata.version
git commit -m "${STAGE} trigon-web deployed with tag ${TAG}, from deploy pipeline"
git push origin $GitAppBranch
