#!/bin/bash
# Usage: scripts/delete-stack.sh [environment] [aws-profile] [aws-region]
ENVIRONMENT=${1:-dev}
AWS_PROFILE=${2:-default}
AWS_REGION=${3:-us-east-1}
STACK_NAME=s3-file-processor-$ENVIRONMENT

echo "[INFO] Deleting CloudFormation stack: $STACK_NAME"
aws cloudformation delete-stack --stack-name "$STACK_NAME" --profile "$AWS_PROFILE" --region "$AWS_REGION" || exit 1

echo "[INFO] Waiting for stack deletion to complete..."
aws cloudformation wait stack-delete-complete --stack-name "$STACK_NAME" --profile "$AWS_PROFILE" --region "$AWS_REGION" || exit 1

echo "[INFO] Stack deleted successfully"
