package io.github.anistor.jackpot.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Contributes a fixed percentage of the stake, regardless of pool size.
 */
@Component
class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionType type() {
        return ContributionType.FIXED;
    }

    @Override
    public BigDecimal computeContribution(BigDecimal stake, JackpotConfiguration jackpot) {
        return stake.multiply(jackpot.getContributionRate())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
