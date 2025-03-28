package io.github.zodh.processor.infrastructure.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProcessorExceptionTest {

    @Test
    void testProcessorExceptionWithMessageAndCause() {
        Throwable cause = new Exception("Error");

        ProcessorException exception = new ProcessorException("Custom message", cause);

        assertEquals("Custom message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testProcessorExceptionWithDefaultMessage() {
        ProcessorException exception = new ProcessorException();
        assertEquals("Error trying to process message!", exception.getMessage());
    }
}
