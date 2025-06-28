@echo off
setlocal enabledelayedexpansion

:: S3 File Processor Deployment Script for Windows
:: Usage: scripts\deploy.bat [environment] [aws-profile]

:: Default values
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev

set AWS_PROFILE=%2
if "%AWS_PROFILE%"=="" set AWS_PROFILE=default

:: Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%\..

echo [INFO] Starting deployment for environment: %ENVIRONMENT%
echo [INFO] Using AWS profile: %AWS_PROFILE%

:: Validate environment
if not "%ENVIRONMENT%"=="dev" if not "%ENVIRONMENT%"=="staging" if not "%ENVIRONMENT%"=="prod" (
    echo [ERROR] Invalid environment: %ENVIRONMENT%. Must be dev, staging, or prod.
    exit /b 1
)

:: Set AWS profile
set AWS_PROFILE=%AWS_PROFILE%

:: Validate AWS credentials
echo [INFO] Validating AWS credentials...
aws sts get-caller-identity >nul 2>&1
if errorlevel 1 (
    echo [ERROR] AWS credentials not configured or invalid for profile: %AWS_PROFILE%
    exit /b 1
)

:: Get AWS account info
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
for /f "tokens=*" %%i in ('aws configure get region --profile %AWS_PROFILE% 2^>nul') do set REGION=%%i
if "%REGION%"=="" set REGION=us-east-1

echo [INFO] AWS Account ID: %ACCOUNT_ID%
echo [INFO] AWS Region: %REGION%

:: Navigate to project root
cd /d "%PROJECT_ROOT%"

:: Clean and build the project
echo [INFO] Building the project...
call gradlew.bat clean build -Denv=%ENVIRONMENT% --stacktrace
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

:: Validate CloudFormation templates
echo [INFO] Validating CloudFormation templates...
call gradlew.bat validateTemplates -Denv=%ENVIRONMENT% --stacktrace
if errorlevel 1 (
    echo [ERROR] Template validation failed
    exit /b 1
)

:: Create deployment bucket if it doesn't exist
set DEPLOYMENT_BUCKET=s3-processor-deployment-%ENVIRONMENT%-%ACCOUNT_ID%
echo [INFO] Checking deployment bucket: %DEPLOYMENT_BUCKET%

aws s3 ls "s3://%DEPLOYMENT_BUCKET%" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Creating deployment bucket: %DEPLOYMENT_BUCKET%
    aws s3 mb "s3://%DEPLOYMENT_BUCKET%" --region %REGION%
    if errorlevel 1 (
        echo [ERROR] Failed to create deployment bucket
        exit /b 1
    )
    
    :: Enable versioning
    aws s3api put-bucket-versioning --bucket %DEPLOYMENT_BUCKET% --versioning-configuration Status=Enabled
) else (
    echo [INFO] Deployment bucket already exists
)

:: Package Lambda functions
echo [INFO] Packaging Lambda functions...
call gradlew.bat packageLambda -Denv=%ENVIRONMENT%
if errorlevel 1 (
    echo [ERROR] Lambda packaging failed
    exit /b 1
)

:: Upload Lambda package to S3
for /f "tokens=2 delims= " %%i in ('date /t') do set DATE=%%i
for /f "tokens=1 delims= " %%i in ('time /t') do set TIME=%%i
set TIMESTAMP=%DATE:/=%%_%TIME::=%
set TIMESTAMP=%TIMESTAMP: =%
set LAMBDA_KEY=lambda-packages/s3-processor-%TIMESTAMP%.zip

echo [INFO] Uploading Lambda package to s3://%DEPLOYMENT_BUCKET%/%LAMBDA_KEY%
aws s3 cp build\distributions\lambda-deployment.zip "s3://%DEPLOYMENT_BUCKET%/%LAMBDA_KEY%"
if errorlevel 1 (
    echo [ERROR] Failed to upload Lambda package
    exit /b 1
)

:: Generate unique bucket name for the environment
set S3_BUCKET_NAME=s3-file-processor-%ENVIRONMENT%-%ACCOUNT_ID%

:: Deploy CloudFormation stack
set STACK_NAME=s3-file-processor-%ENVIRONMENT%
echo [INFO] Deploying CloudFormation stack: %STACK_NAME%

aws cloudformation deploy ^
    --template-file src\main\resources\cloudformation\main-stack.yml ^
    --stack-name %STACK_NAME% ^
    --parameter-overrides ^
        Environment=%ENVIRONMENT% ^
        BucketName=%S3_BUCKET_NAME% ^
        FileThreshold=2000 ^
        BatchSize=100 ^
        ScheduleExpression="rate(10 minutes)" ^
    --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM ^
    --region %REGION%

if errorlevel 1 (
    echo [ERROR] CloudFormation deployment failed
    exit /b 1
)

:: Get stack outputs
echo [INFO] Retrieving stack outputs...
for /f "tokens=*" %%i in ('aws cloudformation describe-stacks --stack-name %STACK_NAME% --query "Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue" --output text --region %REGION%') do set API_ENDPOINT=%%i
for /f "tokens=*" %%i in ('aws cloudformation describe-stacks --stack-name %STACK_NAME% --query "Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue" --output text --region %REGION%') do set BUCKET_NAME=%%i

:: Update Lambda function code
echo [INFO] Updating Lambda function code...
set LAMBDA_FUNCTIONS=s3-monitor-%ENVIRONMENT% file-validation-%ENVIRONMENT% file-batching-%ENVIRONMENT%

for %%f in (%LAMBDA_FUNCTIONS%) do (
    echo [INFO] Updating function: %%f
    aws lambda update-function-code ^
        --function-name %%f ^
        --s3-bucket %DEPLOYMENT_BUCKET% ^
        --s3-key %LAMBDA_KEY% ^
        --region %REGION%
    
    if errorlevel 1 (
        echo [WARN] Failed to update function: %%f
    ) else (
        aws lambda wait function-updated --function-name %%f --region %REGION%
    )
)

:: Create initial folder structure in S3 bucket
echo [INFO] Creating initial S3 folder structure...
aws s3api put-object --bucket %BUCKET_NAME% --key "pending/" --region %REGION%
aws s3api put-object --bucket %BUCKET_NAME% --key "processing/" --region %REGION%
aws s3api put-object --bucket %BUCKET_NAME% --key "completed/" --region %REGION%
aws s3api put-object --bucket %BUCKET_NAME% --key "failed/" --region %REGION%

:: Display deployment summary
echo [INFO] ==========================
echo [INFO] Deployment Summary
echo [INFO] ==========================
echo [INFO] Environment: %ENVIRONMENT%
echo [INFO] Stack Name: %STACK_NAME%
echo [INFO] S3 Bucket: %BUCKET_NAME%
echo [INFO] API Endpoint: %API_ENDPOINT%
echo [INFO] AWS Region: %REGION%
echo [INFO] Deployment Bucket: %DEPLOYMENT_BUCKET%
echo [INFO] ==========================

echo [INFO] Deployment completed successfully!

:: Save deployment info to file
echo Deployment Information - %DATE% %TIME% > deployment-info-%ENVIRONMENT%.txt
echo ==================================== >> deployment-info-%ENVIRONMENT%.txt
echo Environment: %ENVIRONMENT% >> deployment-info-%ENVIRONMENT%.txt
echo Stack Name: %STACK_NAME% >> deployment-info-%ENVIRONMENT%.txt
echo S3 Bucket: %BUCKET_NAME% >> deployment-info-%ENVIRONMENT%.txt
echo API Endpoint: %API_ENDPOINT% >> deployment-info-%ENVIRONMENT%.txt
echo AWS Region: %REGION% >> deployment-info-%ENVIRONMENT%.txt
echo AWS Account: %ACCOUNT_ID% >> deployment-info-%ENVIRONMENT%.txt
echo Deployment Bucket: %DEPLOYMENT_BUCKET% >> deployment-info-%ENVIRONMENT%.txt
echo ==================================== >> deployment-info-%ENVIRONMENT%.txt

echo [INFO] Deployment information saved to deployment-info-%ENVIRONMENT%.txt

endlocal
