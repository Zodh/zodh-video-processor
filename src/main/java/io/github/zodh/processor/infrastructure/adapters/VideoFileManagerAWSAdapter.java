package io.github.zodh.processor.infrastructure.adapters;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.core.application.usecases.ExtractVideoFrameUseCase;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.core.domain.VideoCutterMessage;
import io.github.zodh.processor.infrastructure.configuration.AwsVideoServiceConfig;
import io.github.zodh.processor.infrastructure.exception.ProcessorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
public class VideoFileManagerAWSAdapter implements VideoFileManagerGateway {

  private final AwsVideoServiceConfig awsVideoServiceConfig;
  private final ExtractVideoFrameUseCase extractVideoFrameUseCase;
  @Value("${video.bucket-zip-name}")
  private String extractedFramesBucketName;
  @Value("${video.bucket-name}")
  private String rawVideosBucketName;

  public VideoFileManagerAWSAdapter(AwsVideoServiceConfig awsVideoServiceConfig,
      @Lazy ExtractVideoFrameUseCase extractVideoFrameUseCase) {
    this.awsVideoServiceConfig = awsVideoServiceConfig;
    this.extractVideoFrameUseCase = extractVideoFrameUseCase;
  }

  public void deleteVideo(String fileId) {
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(rawVideosBucketName)
          .key(fileId)
          .build();
      awsVideoServiceConfig.getClient().deleteObject(deleteObjectRequest);
    } catch (Exception e) {
      throw new ProcessorException("Error trying to delete S3 object! Object key: " + fileId, e);
    }
  }

  public File downloadVideo(String fileId) {
    try {
      GetObjectRequest request = GetObjectRequest.builder()
          .bucket(rawVideosBucketName)
          .key(fileId)
          .build();

      File temporaryFile = Files.createTempFile("video", ".mp4").toFile();
      downloadFileToMachine(request, temporaryFile);
      return temporaryFile;
    } catch (Exception e) {
      throw new ProcessorException("Error trying to download S3 object! Object key: " + fileId, e);
    }
  }

  private void downloadFileToMachine(GetObjectRequest request, File temporaryFile)
      throws IOException {
    try (InputStream inputStream = awsVideoServiceConfig.getClient().getObject(request);
        FileOutputStream outputStream = new FileOutputStream(temporaryFile)) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
  }

  public String uploadFrames(ExtractedFrames extractedFrames) {
    try {
      PutObjectRequest putObject = PutObjectRequest.builder()
          .bucket(extractedFramesBucketName)
          .key(extractedFrames.zipFileName())
          .acl(ObjectCannedACL.PUBLIC_READ)
          .build();
      awsVideoServiceConfig.getClient()
          .putObject(putObject, RequestBody.fromBytes(extractedFrames.frames()));
      return awsVideoServiceConfig.getClient()
          .utilities()
          .getUrl(
              GetUrlRequest.builder()
                  .bucket(extractedFramesBucketName)
                  .key(extractedFrames.zipFileName())
                  .build()
          ).toString();
    } catch (Exception e) {
      throw new ProcessorException("Error trying to upload a S3 object!", e);
    }
  }

  public void sendStatusUpdate(String fileId, VideoProcessingStatusEnum status, String url) {
    try {
      JSONObject messageToSend = new JSONObject();
      messageToSend.put("fileId", fileId);
      messageToSend.put("status", status);
      if (StringUtils.hasText(url)) {
        messageToSend.put("url", url);
      }

      SendMessageRequest videoStatusUpdateMessage = SendMessageRequest.builder()
          .queueUrl(awsVideoServiceConfig.queueUrl)
          .messageBody(messageToSend.toString())
          .build();
      awsVideoServiceConfig.getSqsClient().sendMessage(videoStatusUpdateMessage);
      log.info("Video status update message sent successfully! File: {} Status: {}", fileId,
          status);
    } catch (Exception e) {
      log.error("Error sending video status update message to SQS queue.", e);
    }
  }

  @Transactional
  @SqsListener("${video.awaiting.processing.queue-name}")
  public void receiveVideoAwaitingProcessing(Message<String> queueMessage) {
    try {
      S3EventNotification eventNotification = S3EventNotification.fromJson(
          new JSONObject(queueMessage.getPayload()).getString("Message"));
      String fileId = eventNotification.getRecords().stream()
          .findFirst()
          .orElseThrow(ProcessorException::new)
          .getS3()
          .getObject()
          .getKey();
      int cutIntervalInSeconds = Integer.parseInt(fileId.split("-")[0]);
      VideoCutterMessage videoCutterMessage = new VideoCutterMessage(fileId, cutIntervalInSeconds);
      extractVideoFrameUseCase.execute(videoCutterMessage);
    } catch (Exception e) {
      log.error("Error trying to process file update message!");
    }
  }
}
