package io.github.anistor.jackpot.controller;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The outcome of a bet. {@code status} is {@code PENDING} until the system has processed the bet,
 * then it transitions to {@code WON} (with a reward amount) or {@code LOST}.
 */
public record RewardResponse(String betId,
                             String jackpotId,
                             BetStatus status,
                             @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal rewardAmount) {

    public RewardResponse {
        if (betId == null) {
            throw new IllegalArgumentException("betId must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (jackpotId == null && (status == BetStatus.WON || status == BetStatus.LOST)) {
            throw new IllegalArgumentException("jackpotId must not be null when status is WON or LOST");
        }
        if (rewardAmount == null && status == BetStatus.WON) {
            throw new IllegalArgumentException("rewardAmount must not be null when status is WON");
        }
    }

    public static RewardResponse pending(String betId) {
        return new RewardResponse(betId, null, BetStatus.PENDING, null);
    }

    public static RewardResponse error(String betId) {
        return new RewardResponse(betId, null, BetStatus.ERROR, null);
    }

    public static RewardResponse lost(String betId, String jackpotId) {
        return new RewardResponse(betId, jackpotId, BetStatus.LOST, null);
    }

    public static RewardResponse won(String betId, String jackpotId, BigDecimal rewardAmount) {
        return new RewardResponse(betId, jackpotId, BetStatus.WON, rewardAmount);
    }
}
