@echo off
setlocal enabledelayedexpansion

:: CloudFormation Template Validation Script
:: Usage: scripts\validate-template.bat [aws-profile]

set AWS_PROFILE=%1
if "%AWS_PROFILE%"=="" set AWS_PROFILE=default

echo [INFO] Validating CloudFormation template...
echo [INFO] Using AWS profile: %AWS_PROFILE%

:: Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%\..
set TEMPLATE_FILE=%PROJECT_ROOT%\src\main\resources\cloudformation\main-stack.yml

:: Navigate to project root
cd /d "%PROJECT_ROOT%"

:: Validate AWS credentials
echo [INFO] Validating AWS credentials...
aws sts get-caller-identity --profile %AWS_PROFILE% >nul 2>&1
if errorlevel 1 (
    echo [ERROR] AWS credentials not configured or invalid for profile: %AWS_PROFILE%
    exit /b 1
)

:: Get region
for /f "tokens=*" %%i in ('aws configure get region --profile %AWS_PROFILE% 2^>nul') do set REGION=%%i
if "%REGION%"=="" set REGION=us-east-1

echo [INFO] AWS Region: %REGION%
echo [INFO] Template file: %TEMPLATE_FILE%

:: Validate template
echo [INFO] Validating CloudFormation template syntax and structure...
aws cloudformation validate-template ^
    --template-body file://%TEMPLATE_FILE% ^
    --profile %AWS_PROFILE% ^
    --region %REGION%

if errorlevel 1 (
    echo [ERROR] CloudFormation template validation failed
    exit /b 1
) else (
    echo [SUCCESS] CloudFormation template is valid
    echo [INFO] Required capabilities: CAPABILITY_IAM, CAPABILITY_NAMED_IAM
    echo [INFO] Template creates named IAM roles for Lambda and Step Functions
    echo [INFO] Deployment will be handled by AWS CLI with proper capabilities
)

echo [INFO] Template validation completed successfully
