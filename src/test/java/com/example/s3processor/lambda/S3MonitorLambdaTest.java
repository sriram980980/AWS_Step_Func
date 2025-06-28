package com.example.s3processor.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.example.s3processor.config.AppConfig;
import com.example.s3processor.model.ProcessingResult;
import com.example.s3processor.service.S3Service;
import com.example.s3processor.service.StepFunctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class S3MonitorLambdaTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private StepFunctionService stepFunctionService;

    @Mock
    private AppConfig config;

    @Mock
    private Context context;

    private S3MonitorLambda lambda;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lambda = new S3MonitorLambda(s3Service, stepFunctionService, config);

        // Setup default config mocks
        when(config.getBucketName()).thenReturn("test-bucket");
        when(config.getPendingPrefix()).thenReturn("pending/");
        when(config.getFileThreshold()).thenReturn(2000);
        when(context.getAwsRequestId()).thenReturn("test-request-id");
    }

    @Test
    void testHandleScheduledEvent_BelowThreshold() {
        // Given
        when(s3Service.countFiles("test-bucket", "pending/")).thenReturn(1500L);

        // When
        ProcessingResult result = lambda.handleScheduledEvent(Map.of(), context);

        // Then
        assertNotNull(result);
        assertEquals(1500L, result.getFileCount());
        assertEquals(2000, result.getThreshold());
        assertFalse(result.isWorkflowTriggered());
        assertNull(result.getStepFunctionExecutionArn());
        verify(stepFunctionService, never()).startFileProcessingWorkflow(anyString(), anyString());
    }

    @Test
    void testHandleScheduledEvent_AboveThreshold() {
        // Given
        when(s3Service.countFiles("test-bucket", "pending/")).thenReturn(2500L);
        when(stepFunctionService.startFileProcessingWorkflow("test-bucket", "pending/"))
                .thenReturn("arn:aws:states:us-east-1:123456789012:execution:test-state-machine:test-execution");

        // When
        ProcessingResult result = lambda.handleScheduledEvent(Map.of(), context);

        // Then
        assertNotNull(result);
        assertEquals(2500L, result.getFileCount());
        assertEquals(2000, result.getThreshold());
        assertTrue(result.isWorkflowTriggered());
        assertEquals("arn:aws:states:us-east-1:123456789012:execution:test-state-machine:test-execution",
                result.getStepFunctionExecutionArn());
        verify(stepFunctionService).startFileProcessingWorkflow("test-bucket", "pending/");
    }

    @Test
    void testHandleScheduledEvent_S3ServiceException() {
        // Given
        when(s3Service.countFiles("test-bucket", "pending/"))
                .thenThrow(new RuntimeException("S3 service error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            lambda.handleScheduledEvent(Map.of(), context);
        });
    }
}
