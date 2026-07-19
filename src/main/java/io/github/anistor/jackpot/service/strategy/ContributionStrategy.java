package io.github.anistor.jackpot.service.strategy;

import java.math.BigDecimal;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Strategy for computing how much of a bet's stake is added to a jackpot pool.
 * Additional contribution schemes can be added by implementing this interface and registering a bean.
 */
public interface ContributionStrategy {

    enum ContributionType {
        FIXED,
        VARIABLE
    }

    ContributionType type();

    BigDecimal computeContribution(BigDecimal stake, JackpotConfiguration jackpot);
}
