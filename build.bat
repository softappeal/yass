@echo off
set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_65
set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:
cmd /c C:\Workarea\_tools\gradle-2.3\bin\gradle.bat -Pversion=%version%
pause
