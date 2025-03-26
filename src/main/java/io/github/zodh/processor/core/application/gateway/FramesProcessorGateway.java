package io.github.zodh.processor.core.application.gateway;

import org.springframework.web.multipart.MultipartFile;

public interface FramesProcessorGateway {

    String extractFrames(MultipartFile file, String zipFileName, int intervalSeconds);

}
