@echo off
setlocal enabledelayedexpansion

:: Test script for S3 File Processor
:: Usage: scripts\test.bat [environment]

set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev

echo [INFO] Running tests for environment: %ENVIRONMENT%

:: Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%\..

cd /d "%PROJECT_ROOT%"

echo [INFO] Running unit tests...
call gradlew.bat test
if errorlevel 1 (
    echo [ERROR] Unit tests failed
    exit /b 1
)

echo [INFO] Running integration tests...
call gradlew.bat integrationTest -Denv=%ENVIRONMENT%
if errorlevel 1 (
    echo [ERROR] Integration tests failed
    exit /b 1
)

echo [INFO] Generating test reports...
call gradlew.bat jacocoTestReport

echo [INFO] All tests completed successfully!

:: Open test report if available
if exist "build\reports\tests\test\index.html" (
    echo [INFO] Opening test report...
    start build\reports\tests\test\index.html
)

if exist "build\reports\jacoco\test\html\index.html" (
    echo [INFO] Opening coverage report...
    start build\reports\jacoco\test\html\index.html
)

endlocal
