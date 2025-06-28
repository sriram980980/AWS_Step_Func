package com.example.s3processor.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.s3processor.service.S3Service;
import com.example.s3processor.config.AppConfig;
import com.example.s3processor.model.BatchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda function for file validation in Step Function workflow
 */
public class FileValidationLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(FileValidationLambda.class);
    
    private final S3Service s3Service;
    private final AppConfig config;
    
    public FileValidationLambda() {
        this.config = new AppConfig();
        this.s3Service = new S3Service(config);
    }
    
    // Constructor for testing
    public FileValidationLambda(S3Service s3Service, AppConfig config) {
        this.s3Service = s3Service;
        this.config = config;
    }
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        logger.info("File Validation Lambda invoked - Request ID: {}", context.getAwsRequestId());
        
        try {
            // Parse input from Step Function
            String bucketName = (String) input.get("bucketName");
            String batchPrefix = (String) input.get("batchPrefix");
            
            logger.info("Validating files in bucket: {} with prefix: {}", bucketName, batchPrefix);
            
            // Get list of files in the batch
            List<String> fileKeys = s3Service.listFiles(bucketName, batchPrefix);
            
            Map<String, Object> validationResult = new HashMap<>();
            validationResult.put("bucketName", bucketName);
            validationResult.put("batchPrefix", batchPrefix);
            validationResult.put("totalFiles", fileKeys.size());
            
            // Validate each file
            int validFiles = 0;
            int emptyFiles = 0;
            int errorFiles = 0;
            
            for (String fileKey : fileKeys) {
                try {
                    if (s3Service.isFileEmpty(bucketName, fileKey)) {
                        emptyFiles++;
                        logger.warn("Empty file detected: {}", fileKey);
                    } else {
                        validFiles++;
                    }
                } catch (Exception e) {
                    errorFiles++;
                    logger.error("Error validating file: {}", fileKey, e);
                }
            }
            
            validationResult.put("validFiles", validFiles);
            validationResult.put("emptyFiles", emptyFiles);
            validationResult.put("errorFiles", errorFiles);
            validationResult.put("isValid", emptyFiles == 0 && errorFiles == 0);
            validationResult.put("timestamp", System.currentTimeMillis());
            
            logger.info("Validation completed - Valid: {}, Empty: {}, Errors: {}", 
                       validFiles, emptyFiles, errorFiles);
            
            return validationResult;
            
        } catch (Exception e) {
            logger.error("Error during file validation", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("errorMessage", e.getMessage());
            errorResult.put("timestamp", System.currentTimeMillis());
            
            return errorResult;
        }
    }
    
    /**
     * Validate a specific batch of files
     */
    public Map<String, Object> validateBatch(BatchRequest batchRequest, Context context) {
        Map<String, Object> input = new HashMap<>();
        input.put("bucketName", batchRequest.getBucketName());
        input.put("batchPrefix", batchRequest.getBatchPrefix());
        
        return handleRequest(input, context);
    }
}
