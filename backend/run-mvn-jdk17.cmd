@echo off
setlocal
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
mvn -s maven-central-settings.xml %*
