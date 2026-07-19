package io.github.anistor.jackpot.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.anistor.jackpot.domain.JackpotEntity;

class ContributionStrategyTest {

    private JackpotEntity jackpot(ContributionStrategy.ContributionType type, BigDecimal rate, BigDecimal limit, BigDecimal currentPool) {
        JackpotEntity jackpot = JackpotEntity.builder()
                .id("J")
                .initialPool(BigDecimal.valueOf(1000))
                .contributionType(type)
                .contributionRate(rate)
                .contributionPoolLimit(limit)
                .rewardType(RewardStrategy.RewardType.FIXED)
                .rewardRate(BigDecimal.valueOf(0.01))
                .build();
        jackpot.addContributionToPool(currentPool.subtract(jackpot.getInitialPool()));
        return jackpot;
    }

    @Test
    void fixedContributionIsConstantPercentageOfStake() {
        JackpotEntity jackpot = jackpot(ContributionStrategy.ContributionType.FIXED, BigDecimal.valueOf(0.05), null, BigDecimal.valueOf(1000));

        BigDecimal contribution = new FixedContributionStrategy()
                .computeContribution(BigDecimal.valueOf(200), jackpot);

        assertThat(contribution).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void variableContributionShrinksAsPoolGrows() {
        VariableContributionStrategy strategy = new VariableContributionStrategy();
        BigDecimal stake = BigDecimal.valueOf(100);

        JackpotEntity empty = jackpot(ContributionStrategy.ContributionType.VARIABLE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(10000), BigDecimal.valueOf(0));
        JackpotEntity half = jackpot(ContributionStrategy.ContributionType.VARIABLE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(10000), BigDecimal.valueOf(5000));

        BigDecimal atEmpty = strategy.computeContribution(stake, empty);
        BigDecimal atHalf = strategy.computeContribution(stake, half);

        assertThat(atEmpty).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(atHalf).isEqualByComparingTo(BigDecimal.valueOf(5))
                .isLessThan(atEmpty);
    }

    @Test
    void variableContributionReachesZeroAtLimit() {
        JackpotEntity full = jackpot(ContributionStrategy.ContributionType.VARIABLE, BigDecimal.valueOf(0.10), BigDecimal.valueOf(10000.0), BigDecimal.valueOf(10000.0));

        BigDecimal contribution = new VariableContributionStrategy()
                .computeContribution(BigDecimal.valueOf(100), full);

        assertThat(contribution).isEqualByComparingTo(BigDecimal.valueOf(0.0));
    }
}
