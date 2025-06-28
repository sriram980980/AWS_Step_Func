package com.example.s3processor.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.example.s3processor.service.S3Service;
import com.example.s3processor.service.StepFunctionService;
import com.example.s3processor.config.AppConfig;
import com.example.s3processor.model.ProcessingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Lambda function to monitor S3 bucket and trigger processing workflow
 */
public class S3MonitorLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(S3MonitorLambda.class);
    
    private final S3Service s3Service;
    private final StepFunctionService stepFunctionService;
    private final AppConfig config;
    private final ObjectMapper objectMapper;
    
    public S3MonitorLambda() {
        this.config = new AppConfig();
        this.s3Service = new S3Service(config);
        this.stepFunctionService = new StepFunctionService(config);
        this.objectMapper = new ObjectMapper();
    }
    
    // Constructor for testing
    public S3MonitorLambda(S3Service s3Service, StepFunctionService stepFunctionService, AppConfig config) {
        this.s3Service = s3Service;
        this.stepFunctionService = stepFunctionService;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        logger.info("S3 Monitor Lambda invoked - Request ID: {}", context.getAwsRequestId());
        
        try {
            ProcessingResult result = processS3Monitoring();
            
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setHeaders(createCorsHeaders());
            response.setBody(objectMapper.writeValueAsString(result));
            
            logger.info("S3 Monitor Lambda completed successfully");
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing S3 monitoring request", e);
            
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setHeaders(createCorsHeaders());
            
            try {
                Map<String, String> errorBody = new HashMap<>();
                errorBody.put("error", "Internal server error");
                errorBody.put("message", e.getMessage());
                errorResponse.setBody(objectMapper.writeValueAsString(errorBody));
            } catch (Exception jsonException) {
                errorResponse.setBody("{\"error\":\"Internal server error\"}");
            }
            
            return errorResponse;
        }
    }
    
    /**
     * Core logic for monitoring S3 and triggering processing
     */
    private ProcessingResult processS3Monitoring() {
        String bucketName = config.getBucketName();
        String pendingPrefix = config.getPendingPrefix();
        int fileThreshold = config.getFileThreshold();
        
        logger.info("Checking S3 bucket: {} with prefix: {} for threshold: {}", 
                   bucketName, pendingPrefix, fileThreshold);
        
        // Count files in pending folder
        long fileCount = s3Service.countFiles(bucketName, pendingPrefix);
        logger.info("Found {} files in pending folder", fileCount);
        
        ProcessingResult result = new ProcessingResult();
        result.setFileCount(fileCount);
        result.setThreshold(fileThreshold);
        result.setBucketName(bucketName);
        result.setTimestamp(System.currentTimeMillis());
        
        if (fileCount >= fileThreshold) {
            logger.info("File threshold exceeded. Triggering Step Function workflow");
            
            // Trigger Step Function for file processing
            String executionArn = stepFunctionService.startFileProcessingWorkflow(bucketName, pendingPrefix);
            result.setStepFunctionExecutionArn(executionArn);
            result.setWorkflowTriggered(true);
            
            logger.info("Step Function workflow started with execution ARN: {}", executionArn);
        } else {
            logger.info("File threshold not reached. No action taken");
            result.setWorkflowTriggered(false);
        }
        
        return result;
    }
    
    /**
     * Handler for scheduled invocations (CloudWatch Events)
     */
    public ProcessingResult handleScheduledEvent(Map<String, Object> event, Context context) {
        logger.info("S3 Monitor Lambda invoked via schedule - Request ID: {}", context.getAwsRequestId());
        return processS3Monitoring();
    }
    
    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
}
