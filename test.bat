echo off

docker rm --force yass
docker image rm --force yass

docker build --tag yass .

docker create --name yass --tty --interactive yass /bin/bash

docker cp C:\Users\guru\OneDrive\data\essential\development\AngeloSalvade.MavenCentral.SigningKey\gradle.properties yass:/yass
docker cp C:\Users\guru\OneDrive\data\essential\development\AngeloSalvade.MavenCentral.SigningKey\maven.central.key.gpg yass:/

docker start --attach --interactive yass
