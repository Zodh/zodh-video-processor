package io.github.zodh.processor.infrastructure.configuration;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class AwsVideoServiceConfigTest {

    @InjectMocks
    private AwsVideoServiceConfig awsVideoServiceConfig;

    @Mock
    private AwsCredentialsProvider awsCredentialsProvider;

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        awsVideoServiceConfig = new AwsVideoServiceConfig();
        ReflectionTestUtils.setField(awsVideoServiceConfig, "region", "mock-region");
        ReflectionTestUtils.setField(awsVideoServiceConfig, "accessKey", "mock-access-key");
        ReflectionTestUtils.setField(awsVideoServiceConfig, "secretKey", "mock-secret-key");
        ReflectionTestUtils.setField(awsVideoServiceConfig, "sessionToken", "mock-token");
        ReflectionTestUtils.setField(awsVideoServiceConfig, "queueUrl", "http://example.com");
    }

    @Test
    void testGetS3Client() {
        var mockCredentials = AwsSessionCredentials
                .create("mock-access-key", "mock-secret-key", "mock-token");

        when(awsCredentialsProvider.resolveCredentials()).thenReturn(mockCredentials);

        S3Client s3Client = awsVideoServiceConfig.getClient();

        assertNotNull(s3Client);
    }

    @Test
    void testGetPreSigner() {
        var mockCredentials = AwsSessionCredentials
                .create("mock-access-key", "mock-secret-key", "mock-token");

        when(awsCredentialsProvider.resolveCredentials()).thenReturn(mockCredentials);

        S3Presigner s3Client = awsVideoServiceConfig.getPreSigner();

        assertNotNull(s3Client);
    }

    @Test
    void testSqsTemplate() {
        var mockCredentials = AwsSessionCredentials
                .create("mock-access-key", "mock-secret-key", "mock-token");

        when(awsCredentialsProvider.resolveCredentials()).thenReturn(mockCredentials);

        SqsTemplate sqsTemplate = awsVideoServiceConfig.sqsTemplate(sqsAsyncClient);

        assertNotNull(sqsTemplate);
    }

    @Test
    void testGetSqsClient() {
        var mockCredentials = AwsSessionCredentials
                .create("mock-access-key", "mock-secret-key", "mock-token");

        when(awsCredentialsProvider.resolveCredentials()).thenReturn(mockCredentials);

        SqsClient sqsClient = awsVideoServiceConfig.getSqsClient();

        assertNotNull(sqsClient);
    }

    @Test
    void testSqsAsyncClient() {
        var mockCredentials = AwsSessionCredentials
                .create("mock-access-key", "mock-secret-key", "mock-token");

        when(awsCredentialsProvider.resolveCredentials()).thenReturn(mockCredentials);

        SqsAsyncClient sqsTemplate = awsVideoServiceConfig.sqsAsyncClient();

        assertNotNull(sqsTemplate);
    }
}

