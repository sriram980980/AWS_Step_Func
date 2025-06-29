@echo off
setlocal enabledelayedexpansion

:: Usage: scripts\build-artifacts.bat

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%\..
cd /d "%PROJECT_ROOT%"

call gradlew.bat clean build packageLambda --stacktrace
if errorlevel 1 (
    echo [ERROR] Gradle build/package failed
    exit /b 1
)

echo [INFO] Lambda artifact created at build\distributions\lambda-deployment.zip
endlocal
