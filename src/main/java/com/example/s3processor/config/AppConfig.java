package com.example.s3processor.config;

import software.amazon.awssdk.regions.Region;

/**
 * Application configuration class
 */
public class AppConfig {
    
    // Environment variables and default values
    private static final String DEFAULT_BUCKET_NAME = "s3-file-processor-bucket";
    private static final String DEFAULT_PENDING_PREFIX = "pending/";
    private static final String DEFAULT_PROCESSING_PREFIX = "processing/";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final int DEFAULT_FILE_THRESHOLD = 2000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    public String getBucketName() {
        return getEnvVar("S3_BUCKET_NAME", DEFAULT_BUCKET_NAME);
    }
    
    public String getPendingPrefix() {
        return getEnvVar("PENDING_PREFIX", DEFAULT_PENDING_PREFIX);
    }
    
    public String getProcessingPrefix() {
        return getEnvVar("PROCESSING_PREFIX", DEFAULT_PROCESSING_PREFIX);
    }
    
    public Region getAwsRegion() {
        String regionName = getEnvVar("AWS_REGION", DEFAULT_REGION);
        return Region.of(regionName);
    }
    
    public int getFileThreshold() {
        String threshold = getEnvVar("FILE_THRESHOLD", String.valueOf(DEFAULT_FILE_THRESHOLD));
        try {
            return Integer.parseInt(threshold);
        } catch (NumberFormatException e) {
            return DEFAULT_FILE_THRESHOLD;
        }
    }
    
    public int getBatchSize() {
        String batchSize = getEnvVar("BATCH_SIZE", String.valueOf(DEFAULT_BATCH_SIZE));
        try {
            return Integer.parseInt(batchSize);
        } catch (NumberFormatException e) {
            return DEFAULT_BATCH_SIZE;
        }
    }
    
    public String getFileProcessingStateMachineArn() {
        return getEnvVar("FILE_PROCESSING_STATE_MACHINE_ARN", "");
    }
    
    public String getFileValidationStateMachineArn() {
        return getEnvVar("FILE_VALIDATION_STATE_MACHINE_ARN", "");
    }
    
    public String getApiGatewayEndpoint() {
        return getEnvVar("API_GATEWAY_ENDPOINT", "");
    }
    
    public String getScheduleExpression() {
        return getEnvVar("SCHEDULE_EXPRESSION", "rate(10 minutes)");
    }
    
    public boolean isScheduleEnabled() {
        String enabled = getEnvVar("SCHEDULE_ENABLED", "true");
        return Boolean.parseBoolean(enabled);
    }
    
    public String getEnvironment() {
        return getEnvVar("ENVIRONMENT", "dev");
    }
    
    public String getDeploymentBucket() {
        return getEnvVar("DEPLOYMENT_BUCKET", "deployment-bucket-" + getEnvironment());
    }
    
    /**
     * Get environment variable with default fallback
     */
    private String getEnvVar(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    /**
     * Validate configuration
     */
    public void validateConfig() {
        if (getBucketName().isEmpty()) {
            throw new IllegalStateException("S3 bucket name is required");
        }
        
        if (getFileThreshold() <= 0) {
            throw new IllegalStateException("File threshold must be positive");
        }
        
        if (getBatchSize() <= 0) {
            throw new IllegalStateException("Batch size must be positive");
        }
    }
    
    /**
     * Get configuration summary for logging
     */
    public String getConfigSummary() {
        return String.format(
            "AppConfig{bucket='%s', pendingPrefix='%s', processingPrefix='%s', " +
            "region='%s', fileThreshold=%d, batchSize=%d, environment='%s'}",
            getBucketName(), getPendingPrefix(), getProcessingPrefix(),
            getAwsRegion(), getFileThreshold(), getBatchSize(), getEnvironment()
        );
    }
}
