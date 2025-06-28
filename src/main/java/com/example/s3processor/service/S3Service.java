package com.example.s3processor.service;

import com.example.s3processor.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for S3 operations
 */
public class S3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    private final AppConfig config;
    
    public S3Service(AppConfig config) {
        this.config = config;
        this.s3Client = S3Client.builder()
                .region(config.getAwsRegion())
                .build();
    }
    
    // Constructor for testing
    public S3Service(S3Client s3Client, AppConfig config) {
        this.s3Client = s3Client;
        this.config = config;
    }
    
    /**
     * Count files in a specific S3 prefix
     */
    public long countFiles(String bucketName, String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            long count = 0;
            ListObjectsV2Response response;
            
            do {
                response = s3Client.listObjectsV2(request);
                count += response.contents().stream()
                        .filter(obj -> !obj.key().endsWith("/")) // Exclude directories
                        .count();
                
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            } while (response.isTruncated());
            
            return count;
            
        } catch (Exception e) {
            logger.error("Error counting files in bucket: {} with prefix: {}", bucketName, prefix, e);
            throw new RuntimeException("Failed to count files in S3", e);
        }
    }
    
    /**
     * List all files in a specific S3 prefix
     */
    public List<String> listFiles(String bucketName, String prefix) {
        try {
            List<String> fileKeys = new ArrayList<>();
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response;
            
            do {
                response = s3Client.listObjectsV2(request);
                List<String> keys = response.contents().stream()
                        .map(S3Object::key)
                        .filter(key -> !key.endsWith("/")) // Exclude directories
                        .collect(Collectors.toList());
                
                fileKeys.addAll(keys);
                
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            } while (response.isTruncated());
            
            Collections.sort(fileKeys); // Sort for consistent batch ordering
            return fileKeys;
            
        } catch (Exception e) {
            logger.error("Error listing files in bucket: {} with prefix: {}", bucketName, prefix, e);
            throw new RuntimeException("Failed to list files in S3", e);
        }
    }
    
    /**
     * Move files from source prefix to destination prefix in batches
     */
    public List<String> moveFilesInBatches(String bucketName, String sourcePrefix, String destPrefix) {
        try {
            List<String> sourceFiles = listFiles(bucketName, sourcePrefix);
            int batchSize = config.getBatchSize();
            List<String> batchPrefixes = new ArrayList<>();
            
            logger.info("Moving {} files from {} to {} in batches of {}", 
                       sourceFiles.size(), sourcePrefix, destPrefix, batchSize);
            
            for (int i = 0; i < sourceFiles.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, sourceFiles.size());
                List<String> batch = sourceFiles.subList(i, endIndex);
                
                String batchNumber = String.format("%03d", (i / batchSize) + 1);
                String batchPrefix = destPrefix + "batch-" + batchNumber + "/";
                batchPrefixes.add(batchPrefix);
                
                // Move files in this batch
                for (String sourceKey : batch) {
                    String fileName = sourceKey.substring(sourceKey.lastIndexOf("/") + 1);
                    String destKey = batchPrefix + fileName;
                    
                    // Copy file to new location
                    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                            .copySource(bucketName + "/" + sourceKey)
                            .destinationBucket(bucketName)
                            .destinationKey(destKey)
                            .build();
                    
                    s3Client.copyObject(copyRequest);
                    
                    // Delete original file
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(sourceKey)
                            .build();
                    
                    s3Client.deleteObject(deleteRequest);
                }
                
                logger.info("Moved batch {} with {} files to {}", batchNumber, batch.size(), batchPrefix);
            }
            
            return batchPrefixes;
            
        } catch (Exception e) {
            logger.error("Error moving files from {} to {}", sourcePrefix, destPrefix, e);
            throw new RuntimeException("Failed to move files in S3", e);
        }
    }
    
    /**
     * Check if a file is empty
     */
    public boolean isFileEmpty(String bucketName, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(request);
            return response.contentLength() == 0;
            
        } catch (Exception e) {
            logger.error("Error checking if file is empty: {}", key, e);
            throw new RuntimeException("Failed to check file size", e);
        }
    }
    
    /**
     * Get file content as string (for small files)
     */
    public String getFileContent(String bucketName, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return new String(response.readAllBytes());
            
        } catch (Exception e) {
            logger.error("Error reading file content: {}", key, e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }
    
    /**
     * Create S3 bucket if it doesn't exist
     */
    public void createBucketIfNotExists(String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
            logger.info("Bucket {} already exists", bucketName);
            
        } catch (NoSuchBucketException e) {
            logger.info("Creating bucket: {}", bucketName);
            
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.createBucket(createBucketRequest);
            logger.info("Bucket {} created successfully", bucketName);
            
        } catch (Exception e) {
            logger.error("Error checking/creating bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to create bucket", e);
        }
    }
}
