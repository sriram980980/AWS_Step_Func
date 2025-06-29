@echo off
setlocal enabledelayedexpansion

:: Usage: scripts\delete-stack.bat [environment] [aws-profile] [aws-region]
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev
set AWS_PROFILE=%2
if "%AWS_PROFILE%"=="" set AWS_PROFILE=default
set AWS_REGION=%3
if "%AWS_REGION%"=="" set AWS_REGION=us-east-1

set STACK_NAME=s3-file-processor-%ENVIRONMENT%

echo [INFO] Deleting CloudFormation stack: %STACK_NAME%
aws cloudformation delete-stack --stack-name %STACK_NAME% --profile %AWS_PROFILE% --region %AWS_REGION%
if errorlevel 1 exit /b 1

echo [INFO] Waiting for stack deletion to complete...
aws cloudformation wait stack-delete-complete --stack-name %STACK_NAME% --profile %AWS_PROFILE% --region %AWS_REGION%
if errorlevel 1 exit /b 1

echo [INFO] Stack deleted successfully
endlocal
