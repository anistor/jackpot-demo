package io.github.anistor.jackpot.controller;

/**
 * Status of a bet's reward outcome as exposed by the API.
 * {@code PENDING} until the system has processed the bet, then it transitions to {@code WON} or {@code LOST}.
 * The API caller must poll for status change.
 */
public enum BetStatus {
    PENDING,
    WON,
    LOST,
    ERROR
}
