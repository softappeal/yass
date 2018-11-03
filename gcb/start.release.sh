#!/bin/bash

# abort script on error
set -e

/project/gcb/start.commit.sh

# go to project
cd /project

# upload to maven central
pwd
ls -al
./gradlew clean
#./gradlew publish -Pversion=%version%
