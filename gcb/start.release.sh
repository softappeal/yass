#!/bin/bash

# abort script on error
set -e

# gcloud kms encrypt --plaintext-file=key.gpg           --ciphertext-file=key.gpg.enc           --location=global --keyring=yass --key=build
# gcloud kms encrypt --plaintext-file=gradle.properties --ciphertext-file=gradle.properties.enc --location=global --keyring=yass --key=build

# gcloud kms decrypt --plaintext-file=gcb/key.gpg           --ciphertext-file=gcb/key.gpg.enc           --location=global --keyring=yass --key=build
# gcloud kms decrypt --plaintext-file=gcb/gradle.properties --ciphertext-file=gcb/gradle.properties.enc --location=global --keyring=yass --key=build

# run commit script
/project/gcb/start.commit.sh

# go to project
cd /project

# copy secrets
cp gcb/key.gpg.enc ~/.gradle
cp gcb/gradle.properties.enc ~/.gradle

# upload to maven central
./gradlew publish -Pversion=$PROJECT_VERSION
