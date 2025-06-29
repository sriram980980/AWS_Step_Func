@echo off
setlocal enabledelayedexpansion

:: Usage: scripts\validate-template.bat [aws-profile] [aws-region]
set AWS_PROFILE=%1
if "%AWS_PROFILE%"=="" set AWS_PROFILE=default
set AWS_REGION=%2
if "%AWS_REGION%"=="" set AWS_REGION=us-east-1

aws cloudformation validate-template --template-body file://src/main/resources/cloudformation/main-stack.yml --profile %AWS_PROFILE% --region %AWS_REGION%
if errorlevel 1 (
    echo [ERROR] CloudFormation template validation failed
    exit /b 1
)
echo [INFO] CloudFormation template is valid
endlocal
