package com.example.s3processor.service;

import com.example.s3processor.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private AppConfig config;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3Service = new S3Service(s3Client, config);

        when(config.getAwsRegion()).thenReturn(Region.US_EAST_1);
        when(config.getBatchSize()).thenReturn(100);
    }

    @Test
    void testCountFiles_Success() {
        // Given
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("pending/file1.txt").build(),
                        S3Object.builder().key("pending/file2.txt").build(),
                        S3Object.builder().key("pending/").build() // Should be excluded (directory)
                )
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        long count = s3Service.countFiles("test-bucket", "pending/");

        // Then
        assertEquals(2L, count);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void testListFiles_Success() {
        // Given
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("pending/file2.txt").build(),
                        S3Object.builder().key("pending/file1.txt").build(),
                        S3Object.builder().key("pending/").build() // Should be excluded
                )
                .isTruncated(false)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        List<String> files = s3Service.listFiles("test-bucket", "pending/");

        // Then
        assertEquals(2, files.size());
        assertEquals("pending/file1.txt", files.get(0)); // Should be sorted
        assertEquals("pending/file2.txt", files.get(1));
    }

    @Test
    void testIsFileEmpty_EmptyFile() {
        // Given
        HeadObjectResponse response = HeadObjectResponse.builder()
                .contentLength(0L)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

        // When
        boolean isEmpty = s3Service.isFileEmpty("test-bucket", "test-file.txt");

        // Then
        assertTrue(isEmpty);
    }

    @Test
    void testIsFileEmpty_NonEmptyFile() {
        // Given
        HeadObjectResponse response = HeadObjectResponse.builder()
                .contentLength(1024L)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

        // When
        boolean isEmpty = s3Service.isFileEmpty("test-bucket", "test-file.txt");

        // Then
        assertFalse(isEmpty);
    }

    @Test
    void testCountFiles_S3Exception() {
        // Given
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("Access denied").build());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            s3Service.countFiles("test-bucket", "pending/");
        });

        assertTrue(exception.getMessage().contains("Failed to count files in S3"));
    }
}
