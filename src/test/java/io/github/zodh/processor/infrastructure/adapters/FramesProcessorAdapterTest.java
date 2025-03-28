package io.github.zodh.processor.infrastructure.adapters;

import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.infrastructure.exception.ProcessorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

class FramesProcessorAdapterTest {

    private FramesProcessorAdapter framesProcessorAdapter;

    @TempDir
    Path tempDir;

    private String testVideoPath;

    @BeforeEach
    void setUp() throws Exception {
        framesProcessorAdapter = new FramesProcessorAdapter();
        Path testVideo = tempDir.resolve("test-video.mp4");
        Files.copy(getClass().getResourceAsStream("/sample.mp4"), testVideo, StandardCopyOption.REPLACE_EXISTING);
        testVideoPath = testVideo.toString();
    }

    @Test
    void shouldExtractFramesSuccessfully() {
        ExtractedFrames extractedFrames = framesProcessorAdapter.extractFrames(testVideoPath, "output.zip", 2);

        assertNotNull(extractedFrames, "ExtractedFrames não deve ser nulo");
        assertEquals("output.zip", extractedFrames.zipFileName(), "O nome do arquivo ZIP deve ser 'output.zip'");
        assertNotNull(extractedFrames.frames(), "Os bytes do ZIP não devem ser nulos");
        assertTrue(extractedFrames.frames().length > 0, "O arquivo ZIP deve conter dados");
    }

    @Test
    void shouldThrowProcessorExceptionWhenFileIsInvalid() {
        ProcessorException exception = assertThrows(ProcessorException.class, () ->
                framesProcessorAdapter.extractFrames("invalid/path.mp4", "output.zip", 2));

        assertTrue(exception.getMessage().contains("Error trying to extract video frames"), "A mensagem da exceção deve indicar erro na extração");
    }
}
