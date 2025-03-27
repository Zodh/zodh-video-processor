package io.github.zodh.processor.infrastructure.exception;

public class ProcessorException extends RuntimeException {

  public ProcessorException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessorException() {
    super("Error trying to process message!");
  }
}
