package io.github.anistor.jackpot.service;

public class DuplicateBetException extends RuntimeException {

    public DuplicateBetException(String betId, Throwable cause) {
        super("Duplicate bet: " + betId, cause);
    }
}
