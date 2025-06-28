@echo off
setlocal enabledelayedexpansion

:: Upload test files to S3 for testing
:: Usage: scripts\upload-test-files.bat [environment] [file-count]

set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev

set FILE_COUNT=%2
if "%FILE_COUNT%"=="" set FILE_COUNT=2100

echo [INFO] Uploading %FILE_COUNT% test files to %ENVIRONMENT% environment

:: Get bucket name from CloudFormation stack
for /f "tokens=*" %%i in ('aws cloudformation describe-stacks --stack-name s3-file-processor-%ENVIRONMENT% --query "Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue" --output text') do set BUCKET_NAME=%%i

if "%BUCKET_NAME%"=="" (
    echo [ERROR] Could not find bucket name for environment: %ENVIRONMENT%
    exit /b 1
)

echo [INFO] Using bucket: %BUCKET_NAME%

:: Create temporary directory for test files
if not exist "temp-test-files" mkdir temp-test-files

echo [INFO] Creating %FILE_COUNT% test files...
for /l %%i in (1,1,%FILE_COUNT%) do (
    echo This is test file %%i created at %date% %time% > temp-test-files\test-file-%%i.txt
    if %%i LEQ 10 (
        echo [INFO] Created test file %%i
    ) else if %%i EQU 100 (
        echo [INFO] Created 100 files...
    ) else if %%i EQU 500 (
        echo [INFO] Created 500 files...
    ) else if %%i EQU 1000 (
        echo [INFO] Created 1000 files...
    ) else if %%i EQU 2000 (
        echo [INFO] Created 2000 files...
    )
)

echo [INFO] Uploading files to S3...
aws s3 cp temp-test-files\ s3://%BUCKET_NAME%/pending/ --recursive
if errorlevel 1 (
    echo [ERROR] Failed to upload files to S3
    goto cleanup
)

echo [INFO] Files uploaded successfully!

:: Verify upload
for /f "tokens=*" %%i in ('aws s3 ls s3://%BUCKET_NAME%/pending/ --recursive | find /c "test-file"') do set UPLOADED_COUNT=%%i
echo [INFO] Verified %UPLOADED_COUNT% files in S3 bucket

:: Clean up temporary files
:cleanup
echo [INFO] Cleaning up temporary files...
rmdir /s /q temp-test-files

echo [INFO] Test file upload completed!
echo [INFO] You can now test the processing workflow.

endlocal
