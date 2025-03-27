package io.github.zodh.processor.core.application.usecases;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.core.domain.VideoCutterMessage;
import java.io.File;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ExtractVideoFrameUseCase {

    private final FramesProcessorGateway framesProcessorGateway;
    private final VideoFileManagerGateway videoFileManagerGateway;

    public ExtractVideoFrameUseCase(FramesProcessorGateway framesProcessorGateway, VideoFileManagerGateway videoFileManagerGateway) {
        this.framesProcessorGateway = framesProcessorGateway;
        this.videoFileManagerGateway = videoFileManagerGateway;
    }

    public void execute(VideoCutterMessage videoCutterMessage) {
        String zipFileName = videoCutterMessage.getFileId().replace(".mp4", ".zip");
        try {
            videoFileManagerGateway.sendStatusUpdate(videoCutterMessage.getFileId(), VideoProcessingStatusEnum.PROCESSING, null);
            File downloadedFile = videoFileManagerGateway.downloadVideo(videoCutterMessage.getFileId());
            ExtractedFrames extractedFrames = framesProcessorGateway.extractFrames(downloadedFile.getAbsolutePath(), zipFileName, videoCutterMessage.getCutIntervalInSeconds());
            if (Objects.nonNull(extractedFrames)) {
                String zipDownloadUrl = videoFileManagerGateway.uploadFrames(extractedFrames);
                videoFileManagerGateway.deleteVideo(videoCutterMessage.getFileId());
                videoFileManagerGateway.sendStatusUpdate(videoCutterMessage.getFileId(), VideoProcessingStatusEnum.FINISHED, zipDownloadUrl);
            } else {
                videoFileManagerGateway.sendStatusUpdate(videoCutterMessage.getFileId(), VideoProcessingStatusEnum.ERROR, null);
            }
        } catch (Exception e) {
            videoFileManagerGateway.sendStatusUpdate(videoCutterMessage.getFileId(), VideoProcessingStatusEnum.ERROR, null);
        }
    }
}
