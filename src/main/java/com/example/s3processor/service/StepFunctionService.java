package com.example.s3processor.service;

import com.example.s3processor.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Step Functions operations
 */
public class StepFunctionService {
    
    private static final Logger logger = LoggerFactory.getLogger(StepFunctionService.class);
    
    private final SfnClient stepFunctionsClient;
    private final AppConfig config;
    private final ObjectMapper objectMapper;
    
    public StepFunctionService(AppConfig config) {
        this.config = config;
        this.stepFunctionsClient = SfnClient.builder()
                .region(config.getAwsRegion())
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    // Constructor for testing
    public StepFunctionService(SfnClient stepFunctionsClient, AppConfig config) {
        this.stepFunctionsClient = stepFunctionsClient;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Start the file processing workflow
     */
    public String startFileProcessingWorkflow(String bucketName, String sourcePrefix) {
        try {
            // Prepare input for Step Function
            Map<String, Object> input = new HashMap<>();
            input.put("bucketName", bucketName);
            input.put("sourcePrefix", sourcePrefix);
            input.put("destPrefix", config.getProcessingPrefix());
            input.put("batchSize", config.getBatchSize());
            input.put("timestamp", System.currentTimeMillis());
            
            String inputJson = objectMapper.writeValueAsString(input);
            
            // Start Step Function execution
            StartExecutionRequest request = StartExecutionRequest.builder()
                    .stateMachineArn(config.getFileProcessingStateMachineArn())
                    .input(inputJson)
                    .name("file-processing-" + System.currentTimeMillis())
                    .build();
            
            StartExecutionResponse response = stepFunctionsClient.startExecution(request);
            
            logger.info("Started Step Function execution: {}", response.executionArn());
            return response.executionArn();
            
        } catch (Exception e) {
            logger.error("Error starting Step Function workflow", e);
            throw new RuntimeException("Failed to start Step Function workflow", e);
        }
    }
    
    /**
     * Start the file validation workflow
     */
    public String startFileValidationWorkflow(String bucketName, String batchPrefix) {
        try {
            // Prepare input for Step Function
            Map<String, Object> input = new HashMap<>();
            input.put("bucketName", bucketName);
            input.put("batchPrefix", batchPrefix);
            input.put("timestamp", System.currentTimeMillis());
            
            String inputJson = objectMapper.writeValueAsString(input);
            
            // Start Step Function execution
            StartExecutionRequest request = StartExecutionRequest.builder()
                    .stateMachineArn(config.getFileValidationStateMachineArn())
                    .input(inputJson)
                    .name("file-validation-" + System.currentTimeMillis())
                    .build();
            
            StartExecutionResponse response = stepFunctionsClient.startExecution(request);
            
            logger.info("Started validation Step Function execution: {}", response.executionArn());
            return response.executionArn();
            
        } catch (Exception e) {
            logger.error("Error starting validation Step Function workflow", e);
            throw new RuntimeException("Failed to start validation Step Function workflow", e);
        }
    }
    
    /**
     * Start a batch processing workflow with custom parameters
     */
    public String startBatchProcessingWorkflow(Map<String, Object> parameters) {
        try {
            String inputJson = objectMapper.writeValueAsString(parameters);
            
            StartExecutionRequest request = StartExecutionRequest.builder()
                    .stateMachineArn(config.getFileProcessingStateMachineArn())
                    .input(inputJson)
                    .name("batch-processing-" + System.currentTimeMillis())
                    .build();
            
            StartExecutionResponse response = stepFunctionsClient.startExecution(request);
            
            logger.info("Started batch processing Step Function execution: {}", response.executionArn());
            return response.executionArn();
            
        } catch (Exception e) {
            logger.error("Error starting batch processing Step Function workflow", e);
            throw new RuntimeException("Failed to start batch processing Step Function workflow", e);
        }
    }
}
