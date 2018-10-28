@echo off

call .\gradle

pushd ts
call .\compile
popd

pushd py3
call .\tests
popd

pushd py2
call .\tests
popd

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:
call .\gradle publish -Pversion=%version%

pause
