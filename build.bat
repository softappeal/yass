@echo off
set JAVA_HOME=C:\Program Files (x86)\Java\jdk1.8.0_05
set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:
cmd /c C:\development\gradle-2.0-rc-2\bin\gradle.bat -Pversion=%version%
pause
