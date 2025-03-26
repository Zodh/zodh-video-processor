package io.github.zodh.processor.core.application.usecases;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.core.domain.VideoMessage;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ProcessorVideoUseCase {

    private final FramesProcessorGateway framesProcessorGateway;
    private final VideoFileManagerGateway videoFileManagerGateway;

    public ProcessorVideoUseCase(FramesProcessorGateway framesProcessorGateway, VideoFileManagerGateway videoFileManagerGateway) {
        this.framesProcessorGateway = framesProcessorGateway;
        this.videoFileManagerGateway = videoFileManagerGateway;
    }

    public void execute(VideoMessage videoMessage) {
        var zipFileName = videoMessage.getKeyS3().replace(".mp4", ".zip");
        int intervalSeconds = 10;

        if (Objects.nonNull(videoMessage.getIntervalSeconds())) {
            intervalSeconds = videoMessage.getIntervalSeconds();
        }

        try {
            videoFileManagerGateway.sendStatusUpdateToSQS(zipFileName, VideoProcessingStatusEnum.PROCESSING, null);

            var downloadedFile = videoFileManagerGateway.downloadFile(videoMessage.getKeyS3());
            var extractedFrames = framesProcessorGateway.extractFrames(downloadedFile.getAbsolutePath(), zipFileName, intervalSeconds);

            if (Objects.nonNull(extractedFrames)) {
                var zipUrlS3 = videoFileManagerGateway.sendZipToS3(extractedFrames);
                videoFileManagerGateway.deleteFileFromS3(videoMessage.getKeyS3());

                videoFileManagerGateway.sendStatusUpdateToSQS(videoMessage.getKeyS3(), VideoProcessingStatusEnum.FINISHED, zipUrlS3);
            } else {
                videoFileManagerGateway.sendStatusUpdateToSQS(videoMessage.getKeyS3(), VideoProcessingStatusEnum.ERROR, null);
            }
        } catch (Exception e) {
            videoFileManagerGateway.sendStatusUpdateToSQS(videoMessage.getKeyS3(), VideoProcessingStatusEnum.ERROR, null);
        }
    }
}
