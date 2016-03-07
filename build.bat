@echo off

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

cmd /c npm install -g typescript@1.8.7

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_73

cmd /c C:\development\gradle-2.11\bin\gradle.bat -Pversion=%version%

pause
