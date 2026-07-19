package io.github.anistor.jackpot.service.strategy;

import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Fixed odds: every bet has the same win chance regardless of pool size.
 */
@Component
class FixedRewardStrategy implements RewardStrategy {

    @Override
    public RewardType type() {
        return RewardType.FIXED;
    }

    @Override
    public double computeChance(JackpotConfiguration jackpot) {
        return jackpot.getRewardRate().doubleValue();
    }
}
