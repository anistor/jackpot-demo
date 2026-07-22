package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;

/**
 * Jackpot pool value and configuration parameters, to be used for computing contribution and reward.
 * The actual calculation is performed by the respective strategies,
 * see {@link io.github.anistor.jackpot.service.strategy.ContributionStrategy}
 * and {@link io.github.anistor.jackpot.service.strategy.RewardStrategy}
 */
public interface JackpotConfiguration {

    // TODO Consider moving getCurrentPool() out of this interface to reach immutability
    /**
     * Current pool amount.
     */
    BigDecimal getCurrentPool();

    /**
     * Initial pool amount. Pool returns to this value on reset.
     */
    BigDecimal getInitialPool();

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
