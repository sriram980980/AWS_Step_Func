@echo off
setlocal enabledelayedexpansion

:: Clean up AWS resources for testing
:: Usage: scripts\cleanup.bat [environment]

set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev

echo [INFO] Cleaning up AWS resources for environment: %ENVIRONMENT%

:: Get bucket name from CloudFormation stack
for /f "tokens=*" %%i in ('aws cloudformation describe-stacks --stack-name s3-file-processor-%ENVIRONMENT% --query "Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue" --output text 2^>nul') do set BUCKET_NAME=%%i

if not "%BUCKET_NAME%"=="" (
    echo [INFO] Cleaning S3 bucket: %BUCKET_NAME%
    
    :: Empty the bucket
    echo [INFO] Removing all objects from bucket...
    aws s3 rm s3://%BUCKET_NAME% --recursive
    
    echo [INFO] S3 bucket cleaned successfully
) else (
    echo [WARN] Could not find bucket for environment: %ENVIRONMENT%
)

:: Clean up local build artifacts
echo [INFO] Cleaning local build artifacts...
if exist "build" rmdir /s /q build
if exist ".gradle" rmdir /s /q .gradle
if exist "deployment-info-%ENVIRONMENT%.txt" del "deployment-info-%ENVIRONMENT%.txt"

echo [INFO] Local cleanup completed

:: Optionally delete the CloudFormation stack (commented out for safety)
:: echo [INFO] To delete the entire stack, run:
:: echo aws cloudformation delete-stack --stack-name s3-file-processor-%ENVIRONMENT%

echo [INFO] Cleanup completed!

endlocal
