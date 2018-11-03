#!/bin/bash

# abort script on error
set -e

# check java
java -version

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

# run gradle
chmod +x ./gradlew
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
