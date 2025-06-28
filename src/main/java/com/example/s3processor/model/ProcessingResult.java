package com.example.s3processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class for processing results
 */
public class ProcessingResult {
    
    @JsonProperty("fileCount")
    private long fileCount;
    
    @JsonProperty("threshold")
    private int threshold;
    
    @JsonProperty("bucketName")
    private String bucketName;
    
    @JsonProperty("workflowTriggered")
    private boolean workflowTriggered;
    
    @JsonProperty("stepFunctionExecutionArn")
    private String stepFunctionExecutionArn;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("environment")
    private String environment;
    
    @JsonProperty("message")
    private String message;
    
    public ProcessingResult() {
    }
    
    public ProcessingResult(long fileCount, int threshold, String bucketName) {
        this.fileCount = fileCount;
        this.threshold = threshold;
        this.bucketName = bucketName;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public long getFileCount() {
        return fileCount;
    }
    
    public void setFileCount(long fileCount) {
        this.fileCount = fileCount;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
    
    public String getBucketName() {
        return bucketName;
    }
    
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    
    public boolean isWorkflowTriggered() {
        return workflowTriggered;
    }
    
    public void setWorkflowTriggered(boolean workflowTriggered) {
        this.workflowTriggered = workflowTriggered;
    }
    
    public String getStepFunctionExecutionArn() {
        return stepFunctionExecutionArn;
    }
    
    public void setStepFunctionExecutionArn(String stepFunctionExecutionArn) {
        this.stepFunctionExecutionArn = stepFunctionExecutionArn;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "ProcessingResult{" +
                "fileCount=" + fileCount +
                ", threshold=" + threshold +
                ", bucketName='" + bucketName + '\'' +
                ", workflowTriggered=" + workflowTriggered +
                ", stepFunctionExecutionArn='" + stepFunctionExecutionArn + '\'' +
                ", timestamp=" + timestamp +
                ", environment='" + environment + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
