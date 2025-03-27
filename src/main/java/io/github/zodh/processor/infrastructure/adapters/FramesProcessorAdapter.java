package io.github.zodh.processor.infrastructure.adapters;

import static javax.imageio.ImageIO.write;

import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import io.github.zodh.processor.core.domain.ExtractedFrames;
import io.github.zodh.processor.infrastructure.exception.ProcessorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

@Component
public class FramesProcessorAdapter implements FramesProcessorGateway {

  public ExtractedFrames extractFrames(String path, String zipFileName, int cutIntervalInSeconds) {
    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(path);
        ByteArrayOutputStream zipByteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(zipByteArrayOutputStream);
        Java2DFrameConverter frameConverter = new Java2DFrameConverter()) {

      grabber.start();

      int framesPerSecond = (int) grabber.getVideoFrameRate();
      int frameInterval = framesPerSecond * cutIntervalInSeconds;
      int currentFrame = 0;
      Frame frame;
      while ((frame = grabber.grabImage()) != null) {
        if (currentFrame % frameInterval == 0) {
          BufferedImage bufferedImage = frameConverter.getBufferedImage(frame);
          try (ByteArrayOutputStream frameByteArrayOutputStream = new ByteArrayOutputStream()) {
            write(bufferedImage, "jpg", frameByteArrayOutputStream);
            byte[] imageBytes = frameByteArrayOutputStream.toByteArray();
            String frameName =
                "video_frame_at_second_" + (currentFrame / frameInterval) * cutIntervalInSeconds
                    + ".jpg";
            zipOutputStream.putNextEntry(new ZipEntry(frameName));
            zipOutputStream.write(imageBytes);
            zipOutputStream.closeEntry();
          }
        }
        currentFrame++;
      }
      grabber.stop();
      zipOutputStream.finish();
      return new ExtractedFrames(zipFileName, zipByteArrayOutputStream.toByteArray());
    } catch (Exception e) {
      throw new ProcessorException("Error trying to extract video frames!", e);
    }
  }
}
