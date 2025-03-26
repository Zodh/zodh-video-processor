package io.github.zodh.processor.core.application.usecases;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
public class ProcessorVideoUseCase {

    private final FramesProcessorGateway framesProcessorGateway;
    private final VideoFileManagerGateway videoFileManagerGateway;

    public ProcessorVideoUseCase(FramesProcessorGateway framesProcessorGateway, VideoFileManagerGateway videoFileManagerGateway) {
        this.framesProcessorGateway = framesProcessorGateway;
        this.videoFileManagerGateway = videoFileManagerGateway;
    }

    public void execute(MultipartFile file) {
        var zipFileName = file.getOriginalFilename().replace(".mp4", ".zip");
        int intervalSeconds = 10;

        try {
            videoFileManagerGateway.sendStatusUpdateToSQS("123", VideoProcessingStatusEnum.PROCESSING);
            var extractedFrames = framesProcessorGateway.extractFrames(file, zipFileName, intervalSeconds);

            if (Objects.nonNull(extractedFrames)) {
                videoFileManagerGateway.sendStatusUpdateToSQS("123", VideoProcessingStatusEnum.FINISHED);
            } else {
                videoFileManagerGateway.sendStatusUpdateToSQS("123", VideoProcessingStatusEnum.ERROR);
            }
        } catch (Exception e) {
            videoFileManagerGateway.sendStatusUpdateToSQS("123", VideoProcessingStatusEnum.ERROR);
        }
    }
}
