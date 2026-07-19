package io.github.anistor.jackpot.messaging;

import java.math.BigDecimal;

/**
 * Immutable message payload published to and consumed from the 'jackpot-bets' topic.
 */
public record Bet(String betId, String userId, String jackpotId, BigDecimal betAmount) {
}
