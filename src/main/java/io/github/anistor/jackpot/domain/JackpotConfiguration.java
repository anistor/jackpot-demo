package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;

// TODO Consider moving getCurrentPool() out of this interface to reach immutability.

/**
 * Jackpot pool value and configuration parameters for computing contribution and reward.
 * The actual calculation if performed by the respective strategies, see {@link io.github.anistor.jackpot.service.strategy.ContributionStrategy}
 * and {@link io.github.anistor.jackpot.service.strategy.RewardStrategy}
 */
public interface JackpotConfiguration {

    /**
     * Current pool amount.
     */
    BigDecimal getCurrentPool();

    /**
     * Base contribution rate, right after pool reset.
     */
    BigDecimal getContributionRate();

    /**
     * For variable contribution strategy, over time contribution becomes lower and when the pool amount reaches
     * this limit, the contribution rate becomes 0%.
     */
    BigDecimal getContributionPoolLimit();

    /**
     * Base chance of reward, right after pool reset.
     */
    BigDecimal getRewardRate();

    /**
     * For variable reward strategy, over time the win chance increases and when the pool amount reaches
     * this limit, the win chance becomes 100%.
     */
    BigDecimal getRewardPoolLimit();
}
