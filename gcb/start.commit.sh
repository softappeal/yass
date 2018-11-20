#!/bin/bash

# abort script on error
set -e

# check java
java -version
$JAVA_11_HOME/bin/java -version

# check node
node --version
npm --version

# check python
source activate py3
python --version
source activate py2
python --version
source deactivate

# go to project
cd /project

# prepare gradle
chmod +x ./gradlew

# check Java 11 compatibility
set OLD_JAVA_HOME=$JAVA_HOME
export JAVA_HOME=$JAVA_11_HOME
./gradlew --version
./gradlew
export JAVA_HOME=$OLD_JAVA_HOME

# run gradle
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

# run py2
pushd py2
chmod +x ./tests
./tests
popd
