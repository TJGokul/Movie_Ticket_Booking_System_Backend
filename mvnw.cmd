@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "MAVEN_VERSION=3.9.8"
set "MAVEN_HOME=%~dp0.mvn\apache-maven-%MAVEN_VERSION%"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto RUN_MAVEN

echo [INFO] Maven not found locally. Downloading Apache Maven %MAVEN_VERSION%...
if not exist "%~dp0.mvn" mkdir "%~dp0.mvn"

curl.exe -fsSL "https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip" -o "%~dp0.mvn\maven.zip"
if errorlevel 1 (
    echo [ERROR] Failed to download Maven via curl.
    exit /b 1
)

tar.exe -xf "%~dp0.mvn\maven.zip" -C "%~dp0.mvn"
if errorlevel 1 (
    powershell -NoProfile -Command "Expand-Archive -Path '%~dp0.mvn\maven.zip' -DestinationPath '%~dp0.mvn' -Force"
)

if exist "%~dp0.mvn\maven.zip" del "%~dp0.mvn\maven.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [ERROR] Maven extraction failed.
    exit /b 1
)

:RUN_MAVEN
"%MAVEN_HOME%\bin\mvn.cmd" %*
