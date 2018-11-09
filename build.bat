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

rem set version=0.0.0
rem set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:
rem call .\gradle publish -Pversion=%version%

pause
