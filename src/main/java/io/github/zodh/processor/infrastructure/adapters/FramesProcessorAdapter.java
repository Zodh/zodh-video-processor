package io.github.zodh.processor.infrastructure.adapters;

import io.github.zodh.processor.core.application.gateway.FramesProcessorGateway;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.imageio.ImageIO.write;

@Component
public class FramesProcessorAdapter implements FramesProcessorGateway {

    private final VideoFileManagerAWSAdapter videoFileManagerAWSAdapter;

    public FramesProcessorAdapter(VideoFileManagerAWSAdapter videoFileManagerAWSAdapter) {
        this.videoFileManagerAWSAdapter = videoFileManagerAWSAdapter;
    }

    public String extractFrames(MultipartFile file, String zipFileName, int intervalSeconds) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getInputStream());
             var arrayOutputStream = new ByteArrayOutputStream();
             var zipOutputStream = new ZipOutputStream(arrayOutputStream);
             var frameConverter = new Java2DFrameConverter()) {

            grabber.start();

            int frameRate = (int) grabber.getFrameRate();
            int frameInterval = frameRate * intervalSeconds;
            int frameNumber = 0;
            Frame frame;

            while ((frame = grabber.grabImage()) != null) {
                if (frameNumber % frameInterval == 0) {
                    BufferedImage bufferedImage = frameConverter.getBufferedImage(frame);
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        write(bufferedImage, "jpg", byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();
                        var fileName = "video_frame_" + String.format("%04d", frameNumber) + ".jpg";
                        zipOutputStream.putNextEntry(new ZipEntry(fileName));
                        zipOutputStream.write(imageBytes);
                        zipOutputStream.closeEntry();
                    }
                }
                frameNumber++;
            }

            grabber.stop();
            zipOutputStream.finish();
            return videoFileManagerAWSAdapter.sendZipToS3(zipFileName, arrayOutputStream.toByteArray());

        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
