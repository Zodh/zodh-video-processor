package io.github.zodh.processor.core.domain;

public class VideoMessage {

    private String keyS3;
    private Integer intervalSeconds;

    public VideoMessage(String keyS3, int intervalSeconds) {
        this.keyS3 = keyS3;
        this.intervalSeconds = intervalSeconds;
    }

    public String getKeyS3() {
        return keyS3;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }
}
