package io.github.zodh.processor.core.application.gateway;

import io.github.zodh.processor.core.domain.ExtractedFrames;

public interface FramesProcessorGateway {

    ExtractedFrames extractFrames(String path, String zipFileName, int cutIntervalInSeconds);

}
