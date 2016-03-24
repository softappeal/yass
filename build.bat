@echo off

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

cmd /c npm install -g typescript@1.8.9

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_77

cmd /c C:\Users\guru\development\gradle-2.12\bin\gradle.bat -Pversion=%version%

pause
