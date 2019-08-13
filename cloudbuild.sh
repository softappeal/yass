#!/bin/bash

chmod +x ./gradlew
./gradlew

pushd ts
chmod +x ./compile
./compile
popd

pushd py3
chmod +x ./tests
./tests
popd
