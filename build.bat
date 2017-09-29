@echo off

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

call .\gradle -Pversion=%version%

pushd ts
call .\compile
popd

pushd py3
call .\tests
popd

pushd py2
call .\tests
popd

pause
