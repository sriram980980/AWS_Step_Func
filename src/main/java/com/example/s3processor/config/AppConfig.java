package com.example.s3processor.config;

import software.amazon.awssdk.regions.Region;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration class
 */
public class AppConfig {
    
    private final Properties properties;
    
    // Default values
    private static final String DEFAULT_BUCKET_NAME = "s3-file-processor-bucket";
    private static final String DEFAULT_PENDING_PREFIX = "pending/";
    private static final String DEFAULT_PROCESSING_PREFIX = "processing/";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final int DEFAULT_FILE_THRESHOLD = 2000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    public AppConfig() {
        this.properties = loadProperties();
    }
    
    private Properties loadProperties() {
        Properties props = new Properties();
        String environment = System.getProperty("env", "dev");
        
        // Load environment-specific properties first
        String configFile = "config-" + environment + ".properties";
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            // Fall back to default properties
        }
        
        // Load default application properties as fallback
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties defaultProps = new Properties();
                defaultProps.load(input);
                // Add default properties only if not already set
                for (String key : defaultProps.stringPropertyNames()) {
                    if (!props.containsKey(key)) {
                        props.setProperty(key, defaultProps.getProperty(key));
                    }
                }
            }
        } catch (IOException e) {
            // Continue with current properties
        }
        
        return props;
    }
    
    public String getBucketName() {
        return getProperty("s3.bucket.name", DEFAULT_BUCKET_NAME);
    }
    
    public String getPendingPrefix() {
        return getProperty("s3.pending.prefix", DEFAULT_PENDING_PREFIX);
    }
    
    public String getProcessingPrefix() {
        return getProperty("s3.processing.prefix", DEFAULT_PROCESSING_PREFIX);
    }
    
    public Region getAwsRegion() {
        String regionName = getProperty("aws.region", DEFAULT_REGION);
        return Region.of(regionName);
    }
    
    public int getFileThreshold() {
        String threshold = getProperty("file.threshold", String.valueOf(DEFAULT_FILE_THRESHOLD));
        try {
            return Integer.parseInt(threshold);
        } catch (NumberFormatException e) {
            return DEFAULT_FILE_THRESHOLD;
        }
    }
    
    public int getBatchSize() {
        String batchSize = getProperty("batch.size", String.valueOf(DEFAULT_BATCH_SIZE));
        try {
            return Integer.parseInt(batchSize);
        } catch (NumberFormatException e) {
            return DEFAULT_BATCH_SIZE;
        }
    }
    
    public String getFileProcessingStateMachineArn() {
        return getProperty("stepfunctions.file.processing.arn", "");
    }
    
    public String getFileValidationStateMachineArn() {
        return getProperty("stepfunctions.file.validation.arn", "");
    }
    
    public String getApiGatewayEndpoint() {
        return getProperty("api.gateway.endpoint", "");
    }
    
    public String getScheduleExpression() {
        return getProperty("schedule.expression", "rate(10 minutes)");
    }
    
    public boolean isScheduleEnabled() {
        String enabled = getProperty("schedule.enabled", "true");
        return Boolean.parseBoolean(enabled);
    }
    
    public String getEnvironment() {
        return getProperty("environment", "dev");
    }
    
    public String getDeploymentBucket() {
        return getProperty("deployment.bucket", "s3-file-processor-" + getEnvironment() + "-auto");
    }
    
    /**
     * Get property with default fallback
     */
    private String getProperty(String key, String defaultValue) {
        // First check environment variables (for Lambda runtime compatibility)
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        
        // Then check properties file
        String propValue = properties.getProperty(key);
        return (propValue != null && !propValue.trim().isEmpty()) ? propValue : defaultValue;
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
