package io.github.zodh.processor.core.application.gateway;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.domain.ExtractedFrames;

import java.io.File;

public interface VideoFileManagerGateway {

    String sendZipToS3(ExtractedFrames extractedFrames);

    void sendStatusUpdateToSQS(String fileId, VideoProcessingStatusEnum status, String url);

    File downloadFile(String key);

    void deleteFileFromS3(String key);
}
