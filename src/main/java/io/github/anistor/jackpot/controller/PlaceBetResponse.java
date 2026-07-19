package io.github.anistor.jackpot.controller;

/**
 * Response for a bet request that was accepted for processing.
 */
public record PlaceBetResponse(String betId,
                               BetStatus status) {
}
