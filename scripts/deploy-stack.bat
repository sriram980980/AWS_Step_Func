@echo off
setlocal enabledelayedexpansion

:: Usage: scripts\deploy-stack.bat [environment] [aws-profile] [aws-region] [deploy-bucket]
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev
set AWS_PROFILE=%2
if "%AWS_PROFILE%"=="" set AWS_PROFILE=dev
set AWS_REGION=%3
if "%AWS_REGION%"=="" set AWS_REGION=us-east-1
set DEPLOY_BUCKET=%4
if "%DEPLOY_BUCKET%"=="" set DEPLOY_BUCKET=s3-file-processor-%ENVIRONMENT%-auto

:: Cleanup: delete stack if it exists before deploy
aws cloudformation describe-stacks --stack-name s3-file-processor-%ENVIRONMENT% --profile %AWS_PROFILE% --region %AWS_REGION% >nul 2>&1
if not errorlevel 1 (
    echo [INFO] Deleting existing CloudFormation stack: s3-file-processor-%ENVIRONMENT%
    aws cloudformation delete-stack --stack-name s3-file-processor-%ENVIRONMENT% --profile %AWS_PROFILE% --region %AWS_REGION%
    echo [INFO] Waiting for stack deletion to complete...
    aws cloudformation wait stack-delete-complete --stack-name s3-file-processor-%ENVIRONMENT% --profile %AWS_PROFILE% --region %AWS_REGION%
    if errorlevel 1 exit /b 1
)

:: Build and package artifacts
call "%~dp0build-artifacts.bat"
if errorlevel 1 exit /b 1

:: Validate template
call "%~dp0validate-template.bat" %AWS_PROFILE% %AWS_REGION%
if errorlevel 1 exit /b 1

:: Ensure deployment bucket exists (robust check and create if missing)
aws s3api head-bucket --bucket %DEPLOY_BUCKET% --profile %AWS_PROFILE% --region %AWS_REGION% >nul 2>&1
if errorlevel 1 (
    for /f "delims=" %%E in ('aws s3api head-bucket --bucket %DEPLOY_BUCKET% --profile %AWS_PROFILE% --region %AWS_REGION% 2^>^&1') do set BUCKET_ERROR=%%E
    echo !BUCKET_ERROR! | findstr /C:"Not Found" >nul
    if not errorlevel 1 (
        echo [INFO] Creating S3 bucket: %DEPLOY_BUCKET%
        if /i "%AWS_REGION%"=="us-east-1" (
            aws s3api create-bucket --bucket %DEPLOY_BUCKET% --region %AWS_REGION% --profile %AWS_PROFILE%
        ) else (
            aws s3api create-bucket --bucket %DEPLOY_BUCKET% --region %AWS_REGION% --create-bucket-configuration LocationConstraint=%AWS_REGION% --profile %AWS_PROFILE%
        )
        if errorlevel 1 exit /b 1
        echo [INFO] Waiting for bucket to be available...
        aws s3api wait bucket-exists --bucket %DEPLOY_BUCKET% --profile %AWS_PROFILE% --region %AWS_REGION%
        if errorlevel 1 exit /b 1
    ) else (
        echo !BUCKET_ERROR! | findstr /C:"Forbidden" >nul
        if not errorlevel 1 (
            echo [ERROR] Bucket %DEPLOY_BUCKET% exists but is owned by another account or you lack permissions.
            exit /b 1
        )
        echo !BUCKET_ERROR! | findstr /C:"Bad Request" >nul
        if not errorlevel 1 (
            echo [ERROR] Bucket name %DEPLOY_BUCKET% is invalid.
            exit /b 1
        )
        rem If error is not Not Found, Forbidden, or Bad Request, assume bucket exists and continue
    )
)

:: Upload artifact to S3
echo [INFO] Uploading Lambda artifact to s3://%DEPLOY_BUCKET%/lambda-deployment.zip
aws s3 cp build\distributions\lambda-deployment.zip s3://%DEPLOY_BUCKET%/lambda-deployment.zip --profile %AWS_PROFILE% --region %AWS_REGION%
if errorlevel 1 exit /b 1

:: Deploy stack
echo [INFO] Deploying CloudFormation stack: s3-file-processor-%ENVIRONMENT%
aws cloudformation deploy --template-file src\main\resources\cloudformation\main-stack.yml --stack-name s3-file-processor-%ENVIRONMENT% --parameter-overrides Environment=%ENVIRONMENT% BucketName=%DEPLOY_BUCKET% --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM --profile %AWS_PROFILE% --region %AWS_REGION%
if errorlevel 1 exit /b 1

echo [INFO] Stack deployed successfully
endlocal
