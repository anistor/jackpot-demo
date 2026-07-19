package io.github.anistor.jackpot.service;

import java.math.BigDecimal;

/**
 * The outcome of a bet as known to the service layer, decoupled from both the {@code
 * ProcessedBetEntity} persistence model and the controller's {@code RewardResponse} wire format.
 * Covers every state the reward-lookup endpoint can observe: the bet was never placed
 * ({@code NOT_FOUND}), placed but not yet processed ({@code PENDING}), or fully processed
 * ({@code WON}/{@code LOST}/{@code ERROR}).
 */
public record BetOutcome(String betId, String jackpotId, Status status, BigDecimal rewardAmount) {

    public enum Status {
        PENDING,
        WON,
        LOST,
        ERROR,
        NOT_FOUND
    }

    public BetOutcome {
        if (betId == null) {
            throw new IllegalArgumentException("betId must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public static BetOutcome notFound(String betId) {
        return new BetOutcome(betId, null, Status.NOT_FOUND, null);
    }

    public static BetOutcome pending(String betId) {
        return new BetOutcome(betId, null, Status.PENDING, null);
    }

    public static BetOutcome won(String betId, String jackpotId, BigDecimal rewardAmount) {
        return new BetOutcome(betId, jackpotId, Status.WON, rewardAmount);
    }

    public static BetOutcome lost(String betId, String jackpotId) {
        return new BetOutcome(betId, jackpotId, Status.LOST, null);
    }

    public static BetOutcome error(String betId, String jackpotId) {
        return new BetOutcome(betId, jackpotId, Status.ERROR, null);
    }
}
