package io.github.anistor.jackpot.service;

/**
 * Thrown when a bet references a jackpot that does not exist.
 */
public class JackpotNotFoundException extends RuntimeException {

    public JackpotNotFoundException(String jackpotId) {
        super("Jackpot not found: " + jackpotId);
    }
}
