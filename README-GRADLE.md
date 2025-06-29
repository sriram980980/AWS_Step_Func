# S3 File Processor - Gradle Build & AWS CLI Deployment

This project uses Gradle for Java build and artifact packaging, and simple scripts for AWS deployment and stack management using the AWS CLI.

## 🏗️ Project Structure

- **build.gradle**: Only builds Lambda/Step Function artifacts (no AWS deployment logic)
- **scripts/**: Contains scripts for build, validate, deploy, and delete operations (both .bat and .sh). The deploy script handles S3 bucket creation and pre-deployment cleanup.
- **src/main/resources/cloudformation/main-stack.yml**: CloudFormation template

## 🚀 Usage

### 1. Build Lambda Artifact

```bash
# Windows
scripts\build-artifacts.bat
# Linux/Mac
./scripts/build-artifacts.sh
```

### 2. Validate CloudFormation Template

```bash
# Windows
scripts\validate-template.bat [aws-profile] [aws-region]
# Linux/Mac
./scripts/validate-template.sh [aws-profile] [aws-region]
```

### 3. Deploy Stack

```bash
# Windows
scripts\deploy-stack.bat [environment] [aws-profile] [aws-region] [deploy-bucket]
# Linux/Mac
./scripts/deploy-stack.sh [environment] [aws-profile] [aws-region] [deploy-bucket]
```

### 4. Delete Stack

```bash
# Windows
scripts\delete-stack.bat [environment] [aws-profile] [aws-region]
# Linux/Mac
./scripts/delete-stack.sh [environment] [aws-profile] [aws-region]
```

## 🔧 Notes
- All AWS operations are performed via the AWS CLI in scripts.
- Gradle is only used for Java build and artifact packaging.
- See each script for parameter details and usage.
