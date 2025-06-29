#!/bin/bash
# Usage: scripts/deploy-stack.sh [environment] [aws-profile] [aws-region] [deploy-bucket]
ENVIRONMENT=${1:-dev}
AWS_PROFILE=${2:-default}
AWS_REGION=${3:-us-east-1}
DEPLOY_BUCKET=${4:-s3-file-processor-$ENVIRONMENT-auto}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ARTIFACT_PATH="$SCRIPT_DIR/../build/distributions/lambda-deployment.zip"
TEMPLATE_PATH="$SCRIPT_DIR/../src/main/resources/cloudformation/main-stack.yml"

# Build and package artifacts
"$SCRIPT_DIR/build-artifacts.sh" || { echo "[ERROR] Build failed"; exit 1; }

# Validate template
"$SCRIPT_DIR/validate-template.sh" "$AWS_PROFILE" "$AWS_REGION" || { echo "[ERROR] Template validation failed"; exit 1; }

# Ensure deployment bucket exists (robust check and create if missing)
BUCKET_EXISTS=$(aws s3api head-bucket --bucket "$DEPLOY_BUCKET" --profile "$AWS_PROFILE" --region "$AWS_REGION" 2>&1)
if echo "$BUCKET_EXISTS" | grep -q 'Not Found'; then
  echo "[INFO] Creating S3 bucket: $DEPLOY_BUCKET"
  if [ "$AWS_REGION" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "$DEPLOY_BUCKET" --region "$AWS_REGION" --profile "$AWS_PROFILE" || { echo "[ERROR] Failed to create bucket"; exit 1; }
  else
    aws s3api create-bucket --bucket "$DEPLOY_BUCKET" --region "$AWS_REGION" --create-bucket-configuration LocationConstraint="$AWS_REGION" --profile "$AWS_PROFILE" || { echo "[ERROR] Failed to create bucket"; exit 1; }
  fi
  # Wait for bucket to exist before continuing
  echo "[INFO] Waiting for bucket to be available..."
  aws s3api wait bucket-exists --bucket "$DEPLOY_BUCKET" --profile "$AWS_PROFILE" --region "$AWS_REGION" || { echo "[ERROR] Bucket did not become available"; exit 1; }
elif echo "$BUCKET_EXISTS" | grep -q 'Forbidden'; then
  echo "[ERROR] Bucket $DEPLOY_BUCKET exists but is owned by another account or you lack permissions."; exit 1
elif echo "$BUCKET_EXISTS" | grep -q 'Bad Request'; then
  echo "[ERROR] Bucket name $DEPLOY_BUCKET is invalid."; exit 1
fi

# Upload artifact to S3
echo "[INFO] Uploading Lambda artifact to s3://$DEPLOY_BUCKET/lambda-deployment.zip"
aws s3 cp "$ARTIFACT_PATH" "s3://$DEPLOY_BUCKET/lambda-deployment.zip" --profile "$AWS_PROFILE" --region "$AWS_REGION" || { echo "[ERROR] Artifact upload failed"; exit 1; }

# Deploy stack
echo "[INFO] Deploying CloudFormation stack: s3-file-processor-$ENVIRONMENT"
aws cloudformation deploy \
  --template-file "$TEMPLATE_PATH" \
  --stack-name "s3-file-processor-$ENVIRONMENT" \
  --parameter-overrides Environment="$ENVIRONMENT" BucketName="$DEPLOY_BUCKET" \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
  --profile "$AWS_PROFILE" --region "$AWS_REGION" || { echo "[ERROR] Stack deployment failed"; exit 1; }

echo "[INFO] Stack deployed successfully"
