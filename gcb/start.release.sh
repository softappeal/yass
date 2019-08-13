#!/bin/bash

# abort script on error
set -e

# run commit script
/project/gcb/start.commit.sh

# go to project
cd /project

# copy secrets
cp gcb/gradle.properties ~/.gradle

# upload to maven central
export JAVA_HOME=$JAVA_11_HOME
./gradlew publish -Pversion=$PROJECT_VERSION
