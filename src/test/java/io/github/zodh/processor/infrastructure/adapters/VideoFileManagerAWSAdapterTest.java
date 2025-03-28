package io.github.zodh.processor.infrastructure.adapters;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.infrastructure.configuration.AwsVideoServiceConfig;
import io.github.zodh.processor.infrastructure.exception.ProcessorException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoFileManagerAWSAdapterTest {

    @Mock
    private AwsVideoServiceConfig awsVideoServiceConfig;

    @Mock
    private S3Client s3Client;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ResponseInputStream<GetObjectResponse> responseInputStream;

    @Mock
    private Message<String> queueMessage;

    @InjectMocks
    private VideoFileManagerAWSAdapter videoFileManagerAWSAdapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(awsVideoServiceConfig.getClient()).thenReturn(s3Client);
        when(awsVideoServiceConfig.getSqsClient()).thenReturn(sqsClient);
    }

    @Test
    void shouldDeleteVideoSuccessfully() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertDoesNotThrow(() -> videoFileManagerAWSAdapter.deleteVideo("test.mp4"));
    }

    @Test
    void shouldThrowExceptionWhenDeleteFails() {
        doThrow(S3Exception.class).when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        assertThrows(ProcessorException.class, () -> videoFileManagerAWSAdapter.deleteVideo("test.mp4"));
    }

    @Test
    void shouldDownloadFileSuccessfully() throws Exception {
        var key = "video.mp4";
        var content = "Test content".getBytes();

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);
        when(responseInputStream.read(any())).thenAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            int length = content.length;
            System.arraycopy(content, 0, buffer, 0, length);
            return -1;
        });
        var downloadedFile = videoFileManagerAWSAdapter.downloadVideo(key);

        assertNotNull(downloadedFile, "Downloaded file should not be null");
        assertTrue(downloadedFile.exists(), "Downloaded file should exist");
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void throwsExceptionWhenDownloadFails() {
        String fileId = "test-file-id";

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        ProcessorException exception = assertThrows(ProcessorException.class, () -> {
            videoFileManagerAWSAdapter.downloadVideo(fileId);
        });

        assertEquals("Error trying to download S3 object! Object key: " + fileId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUploadFails() {
        ExtractedFrames extractedFrames = new ExtractedFrames("output.zip", new byte[]{1, 2, 3});
        doThrow(S3Exception.class).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThrows(ProcessorException.class, () -> videoFileManagerAWSAdapter.uploadFrames(extractedFrames));
    }

    @Test
    void shouldThrowExceptionWhenReceiveEmptyMessage() {
        try {
            var messageContent = "{\"videoKeyS3\":\"10-video.mp4\",\"intervalSeconds\":10}";
            var jsonMessage = new JSONObject().put("Message", messageContent).toString();

            when(queueMessage.getPayload()).thenReturn(jsonMessage);

            videoFileManagerAWSAdapter.receiveVideoAwaitingProcessing(queueMessage);

        } catch (JSONException e) {
            fail("Falha ao criar JSON: " + e.getMessage());
        }
    }

    @Test
    void shouldSendStatusUpdateWithoutUrlSuccessfully() {
        SendMessageRequest videoStatusUpdateMessage = SendMessageRequest.builder()
                .queueUrl(awsVideoServiceConfig.getQueueUrl())
                .messageBody("message")
                .build();

        when(sqsClient.sendMessage(videoStatusUpdateMessage)).thenReturn(mock());

        assertDoesNotThrow(() ->
                videoFileManagerAWSAdapter.sendStatusUpdate("fileId", VideoProcessingStatusEnum.PROCESSING, null));
    }

    @Test
    void shouldSendStatusUpdateWithUrlSuccessfully() {
        SendMessageRequest videoStatusUpdateMessage = SendMessageRequest.builder()
                .queueUrl(awsVideoServiceConfig.getQueueUrl())
                .messageBody("message")
                .build();

        when(sqsClient.sendMessage(videoStatusUpdateMessage)).thenReturn(mock());

        assertDoesNotThrow(() ->
                videoFileManagerAWSAdapter.sendStatusUpdate("fileId", VideoProcessingStatusEnum.PROCESSING, "www.example.com"));
    }
}
