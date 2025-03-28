package io.github.zodh.processor.core.application.usecases;

import io.github.zodh.processor.core.application.enums.VideoProcessingStatusEnum;
import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import io.github.zodh.processor.core.application.gateway.VideoFileManagerGateway;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.core.domain.VideoCutterMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessorVideoUseCaseTest {

    @InjectMocks
    private ExtractVideoFrameUseCase extractVideoFrameUseCase;

    @Mock
    private FramesProcessorGateway framesProcessorGateway;

    @Mock
    private VideoFileManagerGateway videoFileManagerGateway;

    private VideoCutterMessage videoCutterMessage;

    @BeforeEach
    void setUp() {
        videoCutterMessage = new VideoCutterMessage("video.mp4", 5);
    }

    @Test
    void shouldProcessVideoFramesSuccessfully() {
        File mockFile = mock(File.class);
        ExtractedFrames mockExtractedFrames = mock(ExtractedFrames.class);

        when(videoFileManagerGateway.downloadVideo("video.mp4")).thenReturn(mockFile);
        when(mockFile.getAbsolutePath()).thenReturn("/path/to/video.mp4");
        when(framesProcessorGateway.extractFrames("/path/to/video.mp4", "video.zip", 5)).thenReturn(mockExtractedFrames);
        when(videoFileManagerGateway.uploadFrames(mockExtractedFrames)).thenReturn("http://download-link.com/video.zip");

        extractVideoFrameUseCase.execute(videoCutterMessage);

        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.PROCESSING, null);
        verify(videoFileManagerGateway).deleteVideo("video.mp4");
        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.FINISHED, "http://download-link.com/video.zip");
    }

    @Test
    void shouldHandleExtractionFailure() {
        File mockFile = mock(File.class);

        when(videoFileManagerGateway.downloadVideo("video.mp4")).thenReturn(mockFile);
        when(mockFile.getAbsolutePath()).thenReturn("/path/to/video.mp4");
        when(framesProcessorGateway.extractFrames("/path/to/video.mp4", "video.zip", 5)).thenReturn(null);

        extractVideoFrameUseCase.execute(videoCutterMessage);

        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.PROCESSING, null);
        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.ERROR, null);
        verify(videoFileManagerGateway, never()).deleteVideo("video.mp4");
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(videoFileManagerGateway.downloadVideo("video.mp4")).thenThrow(new RuntimeException("Download failed"));

        extractVideoFrameUseCase.execute(videoCutterMessage);

        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.PROCESSING, null);
        verify(videoFileManagerGateway).sendStatusUpdate("video.mp4", VideoProcessingStatusEnum.ERROR, null);
    }
}
