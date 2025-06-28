package com.example.s3processor.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.s3processor.service.S3Service;
import com.example.s3processor.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda function for batching files from pending to processing folder
 */
public class FileBatchingLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(FileBatchingLambda.class);
    
    private final S3Service s3Service;
    private final AppConfig config;
    
    public FileBatchingLambda() {
        this.config = new AppConfig();
        this.s3Service = new S3Service(config);
    }
    
    // Constructor for testing
    public FileBatchingLambda(S3Service s3Service, AppConfig config) {
        this.s3Service = s3Service;
        this.config = config;
    }
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        logger.info("File Batching Lambda invoked - Request ID: {}", context.getAwsRequestId());
        
        try {
            // Parse input from Step Function
            String bucketName = (String) input.get("bucketName");
            String sourcePrefix = (String) input.get("sourcePrefix");
            String destPrefix = (String) input.get("destPrefix");
            
            logger.info("Batching files from bucket: {} source: {} to dest: {}", 
                       bucketName, sourcePrefix, destPrefix);
            
            // Move files in batches
            List<String> batchPrefixes = s3Service.moveFilesInBatches(bucketName, sourcePrefix, destPrefix);
            
            Map<String, Object> result = new HashMap<>();
            result.put("bucketName", bucketName);
            result.put("sourcePrefix", sourcePrefix);
            result.put("destPrefix", destPrefix);
            result.put("batchPrefixes", batchPrefixes);
            result.put("totalBatches", batchPrefixes.size());
            result.put("batchSize", config.getBatchSize());
            result.put("timestamp", System.currentTimeMillis());
            result.put("status", "SUCCESS");
            
            logger.info("File batching completed successfully. Created {} batches", batchPrefixes.size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during file batching", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("errorMessage", e.getMessage());
            errorResult.put("status", "FAILED");
            errorResult.put("timestamp", System.currentTimeMillis());
            
            return errorResult;
        }
    }
}
