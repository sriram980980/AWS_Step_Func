#!/bin/bash

# S3 File Processor Deployment Script
# Usage: ./scripts/deploy.sh [environment] [aws-profile]

set -e  # Exit on any error

# Default values
ENVIRONMENT=${1:-dev}
AWS_PROFILE=${2:-default}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT. Must be dev, staging, or prod."
    exit 1
fi

log_info "Starting deployment for environment: $ENVIRONMENT"
log_info "Using AWS profile: $AWS_PROFILE"

# Set AWS profile
export AWS_PROFILE=$AWS_PROFILE

# Validate AWS credentials
log_info "Validating AWS credentials..."
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    log_error "AWS credentials not configured or invalid for profile: $AWS_PROFILE"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region --profile $AWS_PROFILE || echo "us-east-1")

log_info "AWS Account ID: $ACCOUNT_ID"
log_info "AWS Region: $REGION"

# Navigate to project root
cd "$PROJECT_ROOT"

# Clean and build the project
log_info "Building the project..."
./gradlew clean build -Denv=$ENVIRONMENT

# Validate CloudFormation templates
log_info "Validating CloudFormation templates..."
./gradlew validateTemplates -Denv=$ENVIRONMENT

# Create deployment bucket if it doesn't exist
DEPLOYMENT_BUCKET="s3-processor-deployment-$ENVIRONMENT-$ACCOUNT_ID"
log_info "Checking deployment bucket: $DEPLOYMENT_BUCKET"

if ! aws s3 ls "s3://$DEPLOYMENT_BUCKET" > /dev/null 2>&1; then
    log_info "Creating deployment bucket: $DEPLOYMENT_BUCKET"
    aws s3 mb "s3://$DEPLOYMENT_BUCKET" --region $REGION
    
    # Enable versioning
    aws s3api put-bucket-versioning \
        --bucket $DEPLOYMENT_BUCKET \
        --versioning-configuration Status=Enabled
else
    log_info "Deployment bucket already exists"
fi

# Package Lambda functions
log_info "Packaging Lambda functions..."
./gradlew packageLambda -Denv=$ENVIRONMENT

# Upload Lambda package to S3
LAMBDA_KEY="lambda-packages/s3-processor-$(date +%Y%m%d-%H%M%S).zip"
log_info "Uploading Lambda package to s3://$DEPLOYMENT_BUCKET/$LAMBDA_KEY"

aws s3 cp build/distributions/lambda-deployment.zip \
    "s3://$DEPLOYMENT_BUCKET/$LAMBDA_KEY"

# Generate unique bucket name for the environment
S3_BUCKET_NAME="s3-file-processor-$ENVIRONMENT-$ACCOUNT_ID"

# Deploy CloudFormation stack
STACK_NAME="s3-file-processor-$ENVIRONMENT"
log_info "Deploying CloudFormation stack: $STACK_NAME"

aws cloudformation deploy \
    --template-file src/main/resources/cloudformation/main-stack.yml \
    --stack-name $STACK_NAME \
    --parameter-overrides \
        Environment=$ENVIRONMENT \
        BucketName=$S3_BUCKET_NAME \
        FileThreshold=2000 \
        BatchSize=100 \
        ScheduleExpression="rate(10 minutes)" \
    --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
    --region $REGION

# Get stack outputs
log_info "Retrieving stack outputs..."
API_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiEndpoint`].OutputValue' \
    --output text \
    --region $REGION)

BUCKET_NAME=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --query 'Stacks[0].Outputs[?OutputKey==`BucketName`].OutputValue' \
    --output text \
    --region $REGION)

# Update Lambda function code
log_info "Updating Lambda function code..."
LAMBDA_FUNCTIONS=("s3-monitor-$ENVIRONMENT" "file-validation-$ENVIRONMENT" "file-batching-$ENVIRONMENT")

for FUNCTION_NAME in "${LAMBDA_FUNCTIONS[@]}"; do
    log_info "Updating function: $FUNCTION_NAME"
    aws lambda update-function-code \
        --function-name $FUNCTION_NAME \
        --s3-bucket $DEPLOYMENT_BUCKET \
        --s3-key $LAMBDA_KEY \
        --region $REGION
    
    # Wait for update to complete
    aws lambda wait function-updated \
        --function-name $FUNCTION_NAME \
        --region $REGION
done

# Create initial folder structure in S3 bucket
log_info "Creating initial S3 folder structure..."
aws s3api put-object --bucket $BUCKET_NAME --key "pending/" --region $REGION
aws s3api put-object --bucket $BUCKET_NAME --key "processing/" --region $REGION
aws s3api put-object --bucket $BUCKET_NAME --key "completed/" --region $REGION
aws s3api put-object --bucket $BUCKET_NAME --key "failed/" --region $REGION

# Test the deployment
log_info "Testing the deployment..."
if [ -n "$API_ENDPOINT" ]; then
    log_info "Testing API Gateway endpoint..."
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_ENDPOINT" \
        -H "Content-Type: application/json" \
        -d '{}')
    
    if [ "$HTTP_STATUS" = "200" ]; then
        log_info "API Gateway test successful"
    else
        log_warn "API Gateway test returned status: $HTTP_STATUS"
    fi
else
    log_warn "API endpoint not found in stack outputs"
fi

# Display deployment summary
log_info "=========================="
log_info "Deployment Summary"
log_info "=========================="
log_info "Environment: $ENVIRONMENT"
log_info "Stack Name: $STACK_NAME"
log_info "S3 Bucket: $BUCKET_NAME"
log_info "API Endpoint: $API_ENDPOINT"
log_info "AWS Region: $REGION"
log_info "Deployment Bucket: $DEPLOYMENT_BUCKET"
log_info "=========================="

log_info "Deployment completed successfully!"

# Save deployment info to file
cat > "deployment-info-$ENVIRONMENT.txt" << EOF
Deployment Information - $(date)
====================================
Environment: $ENVIRONMENT
Stack Name: $STACK_NAME
S3 Bucket: $BUCKET_NAME
API Endpoint: $API_ENDPOINT
AWS Region: $REGION
AWS Account: $ACCOUNT_ID
Deployment Bucket: $DEPLOYMENT_BUCKET
====================================
EOF

log_info "Deployment information saved to deployment-info-$ENVIRONMENT.txt"
