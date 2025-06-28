# S3 File Processor - AWS Step Functions Project

A Java-based AWS serverless application that monitors S3 buckets and processes files using Step Functions, Lambda, and API Gateway.

## 🚀 Features

- **Automated S3 Monitoring**: Monitors S3 bucket's `pending/` folder for file threshold
- **Batch Processing**: Automatically moves files to `processing/` folder in configurable batches
- **File Validation**: Validates files for emptiness and other criteria
- **Serverless Architecture**: Uses AWS Lambda, Step Functions, and API Gateway
- **Environment-Specific Deployment**: Supports dev, staging, and production environments
- **CI/CD Ready**: Gradle-based build system with CloudFormation automation
- **Configurable Thresholds**: Customizable file count thresholds and batch sizes
- **REST API**: Exposes endpoints for manual triggering and monitoring

## 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Gateway   │───▶│  S3 Monitor      │───▶│ Step Function 1 │
│                 │    │  Lambda          │    │ (File Batching) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ CloudWatch      │───▶│ Scheduled        │    │ Step Function 2 │
│ Events (Cron)   │    │ Trigger          │    │ (File Validation)│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                       ┌──────────────────┐            ▼
                       │    S3 Bucket     │    ┌─────────────────┐
                       │  ├── pending/    │◀───│ File Batching   │
                       │  ├── processing/ │    │ Lambda          │
                       │  ├── completed/  │    └─────────────────┘
                       │  └── failed/     │            │
                       └──────────────────┘            ▼
                                                ┌─────────────────┐
                                                │ File Validation │
                                                │ Lambda          │
                                                └─────────────────┘
```

## 📋 Prerequisites

- **Java 17** or higher
- **Gradle 8.4** or higher
- **AWS CLI** configured with appropriate credentials
- **AWS Account** with permissions for Lambda, S3, Step Functions, IAM, and CloudFormation

## 🛠️ Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd AWS_Step_Func
```

### 2. Configure AWS Credentials

```bash
aws configure --profile dev
# Enter your AWS Access Key ID, Secret, Region, and Output format
```

### 3. Build the Project

```bash
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

### 4. Deploy to Development Environment

```bash
# Windows
scripts\deploy.bat dev dev

# Linux/Mac
./scripts/deploy.sh dev dev
```

## 🔧 Configuration

### Environment-Specific Configuration

Configuration files are located in `src/main/resources/`:

- `config-dev.properties` - Development environment
- `config-staging.properties` - Staging environment  
- `config-prod.properties` - Production environment

### Key Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `file.threshold` | Minimum files to trigger processing | 2000 |
| `batch.size` | Files per batch | 100 |
| `schedule.expression` | CloudWatch Events cron expression | `rate(10 minutes)` |
| `s3.bucket.name` | S3 bucket name | `s3-file-processor-{env}-bucket` |
| `aws.region` | AWS region | `us-east-1` |

### Environment Variables (Lambda Runtime)

| Variable | Description |
|----------|-------------|
| `S3_BUCKET_NAME` | S3 bucket name |
| `FILE_THRESHOLD` | File count threshold |
| `BATCH_SIZE` | Batch size for processing |
| `ENVIRONMENT` | Environment name (dev/staging/prod) |
| `FILE_PROCESSING_STATE_MACHINE_ARN` | Step Function ARN for processing |
| `FILE_VALIDATION_STATE_MACHINE_ARN` | Step Function ARN for validation |

## 🚀 Deployment

### Automated Deployment

Use the provided deployment scripts for automated deployment:

```bash
# Deploy to development
scripts/deploy.sh dev your-aws-profile

# Deploy to staging  
scripts/deploy.sh staging your-aws-profile

# Deploy to production
scripts/deploy.sh prod your-aws-profile
```

### Manual Deployment Steps

1. **Build the project:**
   ```bash
   ./gradlew clean build -Denv=dev
   ```

2. **Package Lambda functions:**
   ```bash
   ./gradlew packageLambda -Denv=dev
   ```

3. **Deploy CloudFormation stack:**
   ```bash
   ./gradlew deployInfrastructure -Denv=dev
   ```

### Environment-Specific Deployment

```bash
# Development
./gradlew deployDev

# Staging
./gradlew deployStaging  

# Production
./gradlew deployProd
```

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew integrationTest
```

### Manual Testing

1. **Upload test files to S3:**
   ```bash
   # Upload files to pending folder
   aws s3 cp test-files/ s3://your-bucket/pending/ --recursive
   ```

2. **Trigger via API:**
   ```bash
   curl -X POST https://your-api-id.execute-api.region.amazonaws.com/dev/monitor \
        -H "Content-Type: application/json" \
        -d '{}'
   ```

3. **Check Step Function execution:**
   ```bash
   aws stepfunctions list-executions --state-machine-arn your-state-machine-arn
   ```

## 📊 Monitoring

### CloudWatch Logs

- Lambda function logs: `/aws/lambda/function-name`
- Step Function logs: `/aws/stepfunctions/state-machine-name`
- S3 access logs: `/aws/s3/bucket-name`

### CloudWatch Metrics

- Lambda invocations, duration, errors
- Step Function executions, success/failure rates
- S3 object counts and API requests

### Alarms

The CloudFormation template includes basic CloudWatch alarms for:
- Lambda function errors
- Step Function execution failures
- API Gateway 4xx/5xx errors

## 🔨 Development

### Project Structure

```
src/
├── main/
│   ├── java/com/example/s3processor/
│   │   ├── config/           # Configuration classes
│   │   ├── lambda/           # Lambda function handlers
│   │   ├── model/            # Data models
│   │   └── service/          # Business logic services
│   └── resources/
│       ├── cloudformation/   # CloudFormation templates
│       ├── config-*.properties # Environment configs
│       └── logback.xml       # Logging configuration
└── test/                     # Unit and integration tests
```

### Adding New Lambda Functions

1. Create handler class in `com.example.s3processor.lambda`
2. Add function definition to CloudFormation template
3. Update deployment scripts if needed
4. Add corresponding tests

### Modifying Step Functions

1. Update the state machine definition in `main-stack.yml`
2. Test the workflow using AWS Console or CLI
3. Deploy using `./gradlew deployInfrastructure`

## 🔐 Security

### IAM Roles and Policies

The application creates minimal IAM roles with least-privilege access:

- **Lambda Execution Role**: S3 access, Step Functions execution, CloudWatch Logs
- **Step Functions Role**: Lambda invocation permissions
- **API Gateway Role**: Lambda invocation permissions

### S3 Bucket Security

- Public access blocked by default
- Versioning enabled
- Server-side encryption can be configured

### Network Security

- Lambda functions run in AWS managed VPC by default
- API Gateway uses HTTPS only
- CORS headers configured for web access

## 🐛 Troubleshooting

### Common Issues

1. **Deployment fails with permissions error:**
   - Ensure AWS credentials have sufficient permissions
   - Check IAM policies for CloudFormation, Lambda, S3, Step Functions

2. **Lambda function timeout:**
   - Increase timeout in CloudFormation template
   - Optimize code for better performance
   - Check CloudWatch logs for details

3. **Step Function execution fails:**
   - Check individual Lambda function logs
   - Verify IAM permissions for cross-service calls
   - Review Step Function execution history

4. **API Gateway returns 5xx errors:**
   - Check Lambda function logs
   - Verify API Gateway integration configuration
   - Test Lambda function independently

### Debugging Commands

```bash
# Check CloudFormation stack status
aws cloudformation describe-stacks --stack-name s3-file-processor-dev

# View Lambda function logs
aws logs tail /aws/lambda/s3-monitor-dev --follow

# List Step Function executions
aws stepfunctions list-executions --state-machine-arn <arn>

# Test Lambda function
aws lambda invoke --function-name s3-monitor-dev response.json
```

## 📚 API Reference

### REST Endpoints

#### POST /monitor
Manually trigger S3 monitoring and processing workflow.

**Request:**
```json
{}
```

**Response:**
```json
{
  "fileCount": 2500,
  "threshold": 2000,
  "bucketName": "s3-file-processor-dev-bucket",
  "workflowTriggered": true,
  "stepFunctionExecutionArn": "arn:aws:states:...",
  "timestamp": 1672531200000
}
```

### Step Function Input/Output

#### File Processing Workflow Input
```json
{
  "bucketName": "s3-file-processor-bucket",
  "sourcePrefix": "pending/",
  "destPrefix": "processing/",
  "batchSize": 100,
  "timestamp": 1672531200000
}
```

#### File Validation Workflow Input
```json
{
  "bucketName": "s3-file-processor-bucket",
  "batchPrefix": "processing/batch-001/",
  "timestamp": 1672531200000
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:

- Create an issue in the GitHub repository
- Check the troubleshooting section above
- Review AWS documentation for the services used

## 🗺️ Roadmap

- [ ] Add support for multiple file types and validation rules
- [ ] Implement dead letter queues for failed processing
- [ ] Add metrics dashboard using CloudWatch Dashboard
- [ ] Support for VPC deployment
- [ ] Integration with AWS X-Ray for distributed tracing
- [ ] Add support for SNS notifications
- [ ] Implement cost optimization features