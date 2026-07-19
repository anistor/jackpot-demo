package io.github.anistor.jackpot.service.strategy;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Strategy for deciding whether a bet wins the jackpot. The win is rolled immediately after the
 * bet's own contribution is applied, using the pool state that bet just created making
 * the outcome deterministic and independent of any later, unrelated activity.
 * New reward schemes can be added by implementing this interface and registering a bean.
 */
public interface RewardStrategy {

    enum RewardType {
        FIXED,
        VARIABLE
    }

    RewardType type();

    /**
     * Probability in [0, 1] that a bet wins, given the current pool state.
     */
    double computeChance(JackpotConfiguration jackpot);
}
