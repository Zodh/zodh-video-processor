package io.github.zodh.processor.infrastructure.adapters;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.infrastructure.configuration.AwsVideoServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
public class VideoFileManagerAWSAdapter implements VideoFileManagerGateway {

    private final AwsVideoServiceConfig awsVideoServiceConfig;

    @Value("${video.bucket-name}")
    private String bucketZipName;

    public VideoFileManagerAWSAdapter(AwsVideoServiceConfig awsVideoServiceConfig) {
        this.awsVideoServiceConfig = awsVideoServiceConfig;
    }

    public String sendZipToS3(String fileName, byte[] zipData) {
        try {
            var putObject = PutObjectRequest.builder()
                    .bucket(bucketZipName)
                    .key(fileName)
                    .build();

            awsVideoServiceConfig.getClient().putObject(putObject, RequestBody.fromBytes(zipData));

            return awsVideoServiceConfig.getClient()
                    .utilities()
                    .getUrl(
                            GetUrlRequest.builder()
                                    .bucket(bucketZipName)
                                    .key(fileName)
                                    .build()
                    ).toString();

        } catch (Exception e) {
            return null;
        }
    }

    public void sendStatusUpdateToSQS(String fileId, VideoProcessingStatusEnum status) {
        try {
            // Criando o JSON com os dados a serem enviados
            var messageToSend = new JSONObject();
            messageToSend.put("fileId", fileId);
            messageToSend.put("status", status);

            // TODO Arrumar pois não está enviando pra fila
            awsVideoServiceConfig.sqsTemplate(awsVideoServiceConfig.sqsAsyncClient())
                    .send(messageToSend.toString());

            // Log de confirmação
            log.info("Sent message with fileId: {} and status: {}", fileId, status);

        } catch (Exception e) {
            log.error("Error sending message to SQS queue.", e);
        }
    }

    //TODO Ver oq virá da fila para iniciar o processamento de corte
//    @Transactional
//    @SqsListener("${video.awaiting.processing.queue-name}")
//    public void receiveVideoAwaitingProcessing(Message<String> queueMessage) {
//        try {
//            JSONObject messageAsJson = new JSONObject(queueMessage.getPayload());
//            JSONObject payloadAsJson = new JSONObject(messageAsJson.getString("Message"));
//            String fileId = payloadAsJson.getJSONArray("Records")
//                    .getJSONObject(0)
//                    .getJSONObject("s3")
//                    .getJSONObject("object")
//                    .getString("key");
//            videoCutterJpaRepository.updateVideoCutterProcessingStatus(fileId, VideoProcessingStatusEnum.AWAITING_PROCESSING);
//        } catch (Exception e) {
//            log.error("Error trying to process file update message!");
//        }
//    }
}
