package io.github.zodh.processor.core.domain;

public class VideoCutterMessage {

  private final String fileId;
  private final int cutIntervalInSeconds;

  public VideoCutterMessage(String fileId, int cutIntervalInSeconds) {
    this.fileId = fileId;
    this.cutIntervalInSeconds = cutIntervalInSeconds;
  }

  public String getFileId() {
    return fileId;
  }

  public int getCutIntervalInSeconds() {
    return cutIntervalInSeconds;
  }
}
