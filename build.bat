@echo off

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

cmd /c .\gradlew.bat -Pversion=%version%

pushd ts
call compile.bat
popd

pushd py3
call tests.bat
popd

pushd py2
call tests.bat
popd

pause
