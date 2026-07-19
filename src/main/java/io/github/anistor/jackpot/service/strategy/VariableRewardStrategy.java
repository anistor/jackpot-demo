package io.github.anistor.jackpot.service.strategy;

import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Variable odds: the win chance rises as the pool grows, starting from a base rate and reaching
 * 100% once the pool reaches its configured limit. This guarantees the pool is eventually paid
 * out rather than growing without bound.
 */
@Component
class VariableRewardStrategy implements RewardStrategy {

    @Override
    public RewardType type() {
        return RewardType.VARIABLE;
    }

    @Override
    public double computeChance(JackpotConfiguration jackpot) {
        double rate = jackpot.getRewardRate().doubleValue();

        if (jackpot.getRewardPoolLimit() != null && jackpot.getRewardPoolLimit().signum() > 0) {
            double fill = jackpot.getCurrentPool().divide(jackpot.getRewardPoolLimit(), RoundingMode.HALF_UP).doubleValue();
            rate += fill;
        }

        // clamp it to max 1
        return Math.min(rate, 1.0);
    }
}
