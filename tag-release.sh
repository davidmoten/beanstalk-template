#!/bin/bash
if [[ $# -eq 0 ]] ; then
    echo 'Usage:' 
    echo '  tag-release.sh <RELEASE_VERSION>'
    exit 1 
fi
set -e
set -x
git diff --exit-code
NEW_VERSION=$1
NEW_SNAPSHOT_VERSION=$1.1-SNAPSHOT
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$NEW_VERSION
mvn clean install
git commit -am "set pom version to $NEW_VERSION"
git push 
git tag $NEW_VERSION
git push origin $NEW_VERSION
mvn versions:set -DgenerateBackupPoms -DnewVersion=$NEW_SNAPSHOT_VERSION
git commit -am "set pom version to snapshot version $NEW_SNAPSHOT_VERSION"
git push 
