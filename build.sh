#!/bin/bash

set -e

./cloudbuild.sh

./gradlew publish -Pversion=$1
