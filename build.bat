echo off

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

docker rm --force yass
docker image rm --force yass

docker build --tag yass https://github.com/softappeal/yass.git#v%version%

docker create --name yass yass ./build.sh %version%

docker cp C:\Users\guru\OneDrive\data\essential\development\AngeloSalvade.MavenCentral.SigningKey\gradle.properties yass:/yass
docker cp C:\Users\guru\OneDrive\data\essential\development\AngeloSalvade.MavenCentral.SigningKey\maven.central.key.gpg yass:/

docker start --attach yass
