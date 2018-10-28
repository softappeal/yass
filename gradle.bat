@echo off
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_191
rem set JAVA_HOME=C:\Users\guru\development\openjdk-11.0.1_windows-x64
call .\gradlew --no-daemon %*
