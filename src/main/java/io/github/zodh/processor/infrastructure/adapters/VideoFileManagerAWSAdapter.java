package io.github.zodh.processor.infrastructure.adapters;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.core.application.usecases.ProcessorVideoUseCase;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.core.domain.VideoMessage;
import io.github.zodh.processor.infrastructure.configuration.AwsVideoServiceConfig;
import io.github.zodh.processor.infrastructure.exception.ProcessorException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

@Slf4j
@Component
public class VideoFileManagerAWSAdapter implements VideoFileManagerGateway {

    private final AwsVideoServiceConfig awsVideoServiceConfig;
    private final ProcessorVideoUseCase processorVideoUseCase;

    @Value("${video.bucket-zip-name}")
    private String bucketZipName;

    @Value("${video.bucket-name}")
    private String bucketVideoName;

    public VideoFileManagerAWSAdapter(AwsVideoServiceConfig awsVideoServiceConfig,
                                      @Lazy ProcessorVideoUseCase processorVideoUseCase) {
        this.awsVideoServiceConfig = awsVideoServiceConfig;
        this.processorVideoUseCase = processorVideoUseCase;
    }

    public void deleteFileFromS3(String key) {
        try {
            var deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketVideoName)
                    .key(key)
                    .build();

            awsVideoServiceConfig.getClient().deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new ProcessorException("Erro ao deletar arquivo do S3", e);
        }
    }

    public File downloadFile(String key) {
        try {
            var request = GetObjectRequest.builder()
                    .bucket(bucketVideoName)
                    .key(key)
                    .build();

            var tempFile = Files.createTempFile("video", ".mp4").toFile();
            try (InputStream inputStream = awsVideoServiceConfig.getClient().getObject(request);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile;
        } catch (Exception e) {
            throw new ProcessorException("Erro ao baixar vídeo do servidor S3", e);
        }
    }

    public String sendZipToS3(ExtractedFrames extractedFrames) {
        try {
            var putObject = PutObjectRequest.builder()
                    .bucket(bucketZipName)
                    .key(extractedFrames.zipFileName())
                    .build();

            awsVideoServiceConfig.getClient().putObject(putObject, RequestBody.fromBytes(extractedFrames.frames()));

            return awsVideoServiceConfig.getClient()
                    .utilities()
                    .getUrl(
                            GetUrlRequest.builder()
                                    .bucket(bucketZipName)
                                    .key(extractedFrames.zipFileName())
                                    .build()
                    ).toString();

        } catch (Exception e) {
            throw new ProcessorException("Erro ao fazer upload do vídeo para S3", e);
        }
    }

    public void sendStatusUpdateToSQS(String fileId, VideoProcessingStatusEnum status, String url) {
        try {
            var messageToSend = new JSONObject();
            messageToSend.put("fileId", fileId);
            messageToSend.put("status", status);

            if (Objects.nonNull(url)) {
                messageToSend.put("url", url);
            }

            awsVideoServiceConfig.getSqsClient()
                    .sendMessage(SendMessageRequest.builder()
                            .queueUrl(awsVideoServiceConfig.queueUrl)
                            .messageBody(messageToSend.toString())
                            .build());

            // Log de confirmação
            log.info("Sent message with fileId: {} and status: {}", fileId, status);

        } catch (Exception e) {
            log.error("Error sending message to SQS queue.", e);
        }
    }

    @Transactional
    @SqsListener("${video.awaiting.processing.queue-name}")
    public void receiveVideoAwaitingProcessing(Message<String> queueMessage) {
        try {
            var messageAsJson = new JSONObject(queueMessage.getPayload());
            var payloadAsJson = new JSONObject(messageAsJson.getString("Message"));
            var fileId = payloadAsJson.getJSONArray("Records")
                    .getJSONObject(0)
                    .getJSONObject("s3")
                    .getJSONObject("object")
                    .getString("key");

            // TODO: Ajustar intervalseconds
            var videoMessage = new VideoMessage(fileId, 10);

            processorVideoUseCase.execute(videoMessage);
        } catch (Exception e) {
            log.error("Error trying to process file update message!");
        }
    }
}
