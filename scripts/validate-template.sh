#!/bin/bash
# Usage: scripts/validate-template.sh [aws-profile] [aws-region]
AWS_PROFILE=${1:-default}
AWS_REGION=${2:-us-east-1}

aws cloudformation validate-template --template-body file://src/main/resources/cloudformation/main-stack.yml --profile "$AWS_PROFILE" --region "$AWS_REGION"
if [ $? -ne 0 ]; then
    echo "[ERROR] CloudFormation template validation failed"
    exit 1
fi

echo "[INFO] CloudFormation template is valid"
