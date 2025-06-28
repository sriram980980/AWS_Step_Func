package com.example.s3processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class for batch processing requests
 */
public class BatchRequest {
    
    @JsonProperty("bucketName")
    private String bucketName;
    
    @JsonProperty("sourcePrefix")
    private String sourcePrefix;
    
    @JsonProperty("batchPrefix")
    private String batchPrefix;
    
    @JsonProperty("batchNumber")
    private int batchNumber;
    
    @JsonProperty("batchSize")
    private int batchSize;
    
    @JsonProperty("totalFiles")
    private int totalFiles;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    public BatchRequest() {
    }
    
    public BatchRequest(String bucketName, String sourcePrefix, String batchPrefix) {
        this.bucketName = bucketName;
        this.sourcePrefix = sourcePrefix;
        this.batchPrefix = batchPrefix;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getBucketName() {
        return bucketName;
    }
    
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    
    public String getSourcePrefix() {
        return sourcePrefix;
    }
    
    public void setSourcePrefix(String sourcePrefix) {
        this.sourcePrefix = sourcePrefix;
    }
    
    public String getBatchPrefix() {
        return batchPrefix;
    }
    
    public void setBatchPrefix(String batchPrefix) {
        this.batchPrefix = batchPrefix;
    }
    
    public int getBatchNumber() {
        return batchNumber;
    }
    
    public void setBatchNumber(int batchNumber) {
        this.batchNumber = batchNumber;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "BatchRequest{" +
                "bucketName='" + bucketName + '\'' +
                ", sourcePrefix='" + sourcePrefix + '\'' +
                ", batchPrefix='" + batchPrefix + '\'' +
                ", batchNumber=" + batchNumber +
                ", batchSize=" + batchSize +
                ", totalFiles=" + totalFiles +
                ", timestamp=" + timestamp +
                '}';
    }
}
