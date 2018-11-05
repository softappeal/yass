#!/bin/bash

# abort script on error
set -e

# run commit script
/project/gcb/start.commit.sh

# go to project
cd /project

# copy secrets
cp gcb/key.gpg ~/.gradle
cp gcb/gradle.properties ~/.gradle

# upload to maven central
./gradlew publish -Pversion=$PROJECT_VERSION
