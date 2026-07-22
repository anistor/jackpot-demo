package io.github.anistor.jackpot.messaging;

/**
 * Signals that a Kafka message failed to be processed on first attempt and can never be processed successfully
 * and the failure mode is not recoverable (e.g. data format errors).
 */
public class NonRetryableMessageException extends RuntimeException {

    public NonRetryableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
