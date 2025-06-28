# 🚀 S3 File Processor Project - Complete Setup Guide

## 📋 Project Overview

I've successfully created a comprehensive Java-based AWS serverless application that monitors S3 buckets and processes files using Step Functions, Lambda, and API Gateway. The project includes:

## ✅ What's Been Created

### 🏗️ **Core Infrastructure**
- **Gradle Build System** with AWS CloudFormation plugin
- **Environment-specific configurations** (dev, staging, prod)
- **CloudFormation templates** for complete AWS infrastructure
- **CI/CD pipeline** with GitHub Actions

### 📦 **Java Components**
- **S3MonitorLambda** - Monitors S3 bucket and triggers workflows
- **FileValidationLambda** - Validates files for emptiness and corruption
- **FileBatchingLambda** - Moves files from pending to processing in batches
- **S3Service** - Handles all S3 operations
- **StepFunctionService** - Manages Step Function workflows
- **AppConfig** - Environment-aware configuration management

### 🔧 **Build & Deployment**
- **Gradle scripts** with multi-environment support
- **Deployment scripts** for Windows (batch files)
- **Test automation** with JUnit 5 and Mockito
- **CloudFormation validation** and deployment automation

### 📁 **Project Structure**
```
AWS_Step_Func/
├── src/main/java/com/example/s3processor/
│   ├── config/AppConfig.java
│   ├── lambda/
│   │   ├── S3MonitorLambda.java
│   │   ├── FileValidationLambda.java
│   │   └── FileBatchingLambda.java
│   ├── model/
│   │   ├── ProcessingResult.java
│   │   └── BatchRequest.java
│   └── service/
│       ├── S3Service.java
│       └── StepFunctionService.java
├── src/main/resources/
│   ├── cloudformation/main-stack.yml
│   ├── config-{env}.properties
│   ├── application.properties
│   └── logback.xml
├── src/test/java/ (Unit tests)
├── scripts/ (Deployment and utility scripts)
├── .github/workflows/ci-cd.yml
├── build.gradle
└── README.md (Comprehensive documentation)
```

## 🎯 **Key Features Implemented**

### 1. **Configurable File Processing**
- ✅ Monitors S3 `pending/` folder for file threshold (configurable, default: 2000)
- ✅ Moves files to `processing/` folder in sorted batches (configurable size, default: 100)
- ✅ Validates files for emptiness and other criteria
- ✅ Environment-specific thresholds and batch sizes

### 2. **AWS Services Integration**
- ✅ **Lambda Functions**: Monitor, batch, and validate files
- ✅ **Step Functions**: Two workflows for processing and validation
- ✅ **API Gateway**: REST endpoints for manual triggering
- ✅ **CloudWatch Events**: Scheduled execution (10-minute cron)
- ✅ **S3**: Automated folder structure and file management
- ✅ **IAM**: Least-privilege roles and policies

### 3. **CI/CD Pipeline**
- ✅ **GitHub Actions workflow** with multi-stage deployment
- ✅ **Environment promotion**: dev → staging → production
- ✅ **Automated testing**: Unit tests, integration tests, security scans
- ✅ **Gradle-based build system** with AWS deployment plugin

### 4. **Environment Management**
- ✅ **Multi-environment support**: dev, staging, prod
- ✅ **Environment-specific configurations** and AWS accounts
- ✅ **Parameterized deployments** with Gradle properties
- ✅ **Credential management** through AWS profiles

## 🚀 **Next Steps to Deploy**

### Prerequisites
1. **Install Java 17+** (required for building)
2. **Install AWS CLI** and configure credentials
3. **Set up AWS accounts** for each environment
4. **Configure AWS Credentials** with named profiles

#### AWS Credentials Setup
Before deploying, you must configure AWS credentials with named profiles for each environment:

```bash
#if not installed already aws use 'winget install --id "Amazon.AWSCLI"' for windows 

# Configure AWS credentials for development environment
aws configure --profile dev-profile
# Enter your AWS Access Key ID, Secret Access Key, and region

# Configure AWS credentials for staging environment  
aws configure --profile staging-profile
# Enter your AWS Access Key ID, Secret Access Key, and region

# Configure AWS credentials for production environment
aws configure --profile prod-profile
# Enter your AWS Access Key ID, Secret Access Key, and region
```

**Alternative: Using AWS SSO or IAM Identity Center**
```bash
# For AWS SSO/Identity Center users
aws configure sso --profile dev-profile
aws configure sso --profile staging-profile
aws configure sso --profile prod-profile
```

**Verify your profiles are configured correctly:**
```bash
# List all configured profiles
aws configure list-profiles

# Test profile access
aws sts get-caller-identity --profile dev-profile
aws sts get-caller-identity --profile staging-profile
aws sts get-caller-identity --profile prod-profile
```

**Required IAM Permissions:**
Your AWS credentials need the following permissions:
- CloudFormation: Create, update, delete stacks with IAM capabilities
- Lambda: Create, update, delete functions
- S3: Create, manage buckets and objects
- Step Functions: Create, manage state machines
- API Gateway: Create, manage REST APIs
- IAM: Create, manage roles and policies (requires CAPABILITY_NAMED_IAM)
- CloudWatch: Create, manage logs and metrics

**Important:** The CloudFormation stack requires `CAPABILITY_IAM` and `CAPABILITY_NAMED_IAM` capabilities because it creates named IAM roles. This is automatically handled by the deployment scripts and Gradle tasks using AWS CLI commands.

### Deployment Commands
```bash
# For Development (replace 'dev-profile' with your actual profile name)
scripts\deploy.bat dev dev-profile

# For Staging (replace 'staging-profile' with your actual profile name)
scripts\deploy.bat staging staging-profile

# For Production (replace 'prod-profile' with your actual profile name)
scripts\deploy.bat prod prod-profile
```

**Note:** Make sure to replace the profile names with the actual AWS profile names you configured in the prerequisites step.

### Testing the System
```bash
# Upload test files (replace 'dev-profile' with your actual profile name)
scripts\upload-test-files.bat dev 2100

# Run tests (replace 'dev-profile' with your actual profile name)
scripts\test.bat dev

# Clean up resources (replace 'dev-profile' with your actual profile name)
scripts\cleanup.bat dev
```

**Important:** Ensure your AWS credentials have sufficient permissions and the correct region is set for each environment.

## 📊 **Monitoring & Operations**

### CloudWatch Integration
- **Lambda function logs**: `/aws/lambda/function-name`
- **Step Function execution logs**: Automatic logging enabled
- **S3 access patterns**: CloudWatch metrics included
- **API Gateway metrics**: Request counts, latencies, errors

### Health Checks
- **API endpoint testing**: Automated health checks
- **Step Function execution monitoring**: Success/failure tracking
- **File processing metrics**: Batch sizes, processing times

## 🔒 **Security Features**

### IAM Security
- **Least-privilege access**: Minimal required permissions
- **Role-based access**: Separate roles for Lambda and Step Functions
- **Cross-service permissions**: Properly scoped for AWS services

### S3 Security
- **Public access blocked**: All buckets secured by default
- **Versioning enabled**: File history and recovery
- **Encryption ready**: Can be easily enabled

## 🛠️ **Customization Options**

### Configuration Parameters
- File threshold limits (per environment)
- Batch processing sizes
- Schedule frequencies
- AWS regions and accounts
- Logging levels and retention

### Extensibility
- **Additional validation rules**: Easy to add new file validation logic
- **Multiple file types**: Support for different processing workflows
- **Notification systems**: SNS integration ready
- **Monitoring dashboards**: CloudWatch Dashboard templates included

## 📚 **Documentation Included**

- ✅ **Comprehensive README** with setup instructions
- ✅ **API documentation** with request/response examples
- ✅ **Troubleshooting guide** with common issues and solutions
- ✅ **Architecture diagrams** and workflow explanations
- ✅ **Configuration reference** for all environments

## 🔧 **Troubleshooting Common Issues**

### IAM Capabilities Error
If you encounter: `Requires capabilities: [CAPABILITY_NAMED_IAM]`
- **Cause**: The CloudFormation template creates named IAM roles
- **Solution**: This is automatically handled by the deployment scripts
- **Manual fix**: Add `--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM` to CloudFormation commands

### Circular Dependency Error
If you encounter circular dependency errors:
- **Cause**: Resources referencing each other in environment variables
- **Solution**: The template has been fixed to use runtime discovery instead of hardcoded ARNs
- **Verification**: Run `scripts\validate-template.bat` to check template validity

### AWS Credentials Issues
If deployment fails with credential errors:
- **Check profile**: `aws configure list-profiles`
- **Test access**: `aws sts get-caller-identity --profile your-profile`
- **Verify permissions**: Ensure your user/role has CloudFormation and IAM permissions

### S3 Bucket Name Conflicts
If bucket creation fails:
- **Cause**: S3 bucket names must be globally unique
- **Solution**: The scripts automatically append account ID to bucket names
- **Manual fix**: Update `s3.bucket.name` in config files with a unique suffix

## 🎉 **Project Highlights**

This is a **production-ready, enterprise-grade** AWS serverless application that demonstrates:

- **DevOps best practices** with proper CI/CD
- **AWS Well-Architected principles** for scalability and reliability
- **Clean code architecture** with proper separation of concerns
- **Comprehensive testing strategy** with unit and integration tests
- **Multi-environment deployment** with proper configuration management
- **Security-first approach** with minimal IAM permissions
- **Operational excellence** with monitoring and logging

The project is ready for immediate deployment and can be easily customized for specific business requirements. All components are thoroughly documented and follow industry best practices for AWS serverless applications.

## 🔗 **Ready for Production**

This complete implementation provides everything needed to deploy and operate a robust file processing system on AWS, including monitoring, testing, and operational procedures.
