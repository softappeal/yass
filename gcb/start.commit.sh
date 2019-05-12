#!/bin/bash

# abort script on error
set -e

# check java
$JAVA_11_HOME/bin/java -version
$JAVA_8_HOME/bin/java -version

# check node
node --version
npm --version

# check python
source activate py3
python --version
source deactivate

# go to project
cd /project

# prepare gradle
chmod +x ./gradlew

# run gradle
export JAVA_HOME=$JAVA_11_HOME
./gradlew --version
./gradlew

# run gradle
export JAVA_HOME=$JAVA_8_HOME
./gradlew --version
./gradlew

# run ts
pushd ts
chmod +x ./compile
./compile
popd

# run py3
pushd py3
chmod +x ./tests
./tests
popd
