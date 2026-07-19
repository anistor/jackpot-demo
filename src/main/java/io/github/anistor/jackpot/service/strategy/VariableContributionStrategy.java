package io.github.anistor.jackpot.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

/**
 * Contributes a percentage that shrinks linearly as the pool fills up: the fuller the pool
 * relative to its configured limit, the smaller the cut taken from each stake. At (or beyond)
 * the limit the contribution rate reaches zero.
 */
@Component
class VariableContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionType type() {
        return ContributionType.VARIABLE;
    }

    @Override
    public BigDecimal computeContribution(BigDecimal stake, JackpotConfiguration jackpot) {
        BigDecimal baseRate = jackpot.getContributionRate();
        BigDecimal limit = jackpot.getContributionPoolLimit();
        if (limit == null || limit.signum() <= 0) {
            return stake.multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        }
        double fill = Math.min(1.0, jackpot.getCurrentPool().doubleValue() / limit.doubleValue());
        BigDecimal effectiveRate = baseRate.multiply(BigDecimal.valueOf(1.0 - fill));
        return stake.multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);
    }
}
