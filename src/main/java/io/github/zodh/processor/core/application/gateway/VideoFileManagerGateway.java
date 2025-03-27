package io.github.zodh.processor.core.application.gateway;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.domain.ExtractedFrames;

import java.io.File;

public interface VideoFileManagerGateway {

    String uploadFrames(ExtractedFrames extractedFrames);
    void sendStatusUpdate(String fileId, VideoProcessingStatusEnum status, String url);
    File downloadVideo(String fileId);
    void deleteVideo(String fileId);

}
