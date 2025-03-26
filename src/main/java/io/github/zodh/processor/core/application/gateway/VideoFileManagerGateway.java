package io.github.zodh.processor.core.application.gateway;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;

public interface VideoFileManagerGateway {

    String sendZipToS3(String fileName, byte[] zipData);

    void sendStatusUpdateToSQS(String fileId, VideoProcessingStatusEnum status);
}
