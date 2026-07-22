package io.github.anistor.jackpot.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

class ContributionStrategyTest {

    @Test
    void fixedContributionIsConstantPercentageOfStake() {
        JackpotConfiguration jackpot = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(1000))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.01))
                .build();

        BigDecimal contribution = new FixedContributionStrategy()
                .computeContribution(BigDecimal.valueOf(200), jackpot);

        assertThat(contribution).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void variableContributionShrinksAsPoolGrows() {
        VariableContributionStrategy strategy = new VariableContributionStrategy();
        BigDecimal stake = BigDecimal.valueOf(100);

        JackpotConfiguration empty = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(0))
                .contributionRate(BigDecimal.valueOf(0.1))
                .contributionPoolLimit(BigDecimal.valueOf(10000))
                .rewardRate(BigDecimal.valueOf(0.01))
                .build();
        JackpotConfiguration half = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(5000))
                .contributionRate(BigDecimal.valueOf(0.1))
                .contributionPoolLimit(BigDecimal.valueOf(10000))
                .rewardRate(BigDecimal.valueOf(0.01))
                .build();

        BigDecimal atEmpty = strategy.computeContribution(stake, empty);
        BigDecimal atHalf = strategy.computeContribution(stake, half);

        assertThat(atEmpty).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(atHalf).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void variableContributionReachesZeroAtLimit() {
        JackpotConfiguration full = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(10000.0))
                .contributionRate(BigDecimal.valueOf(0.10))
                .contributionPoolLimit(BigDecimal.valueOf(10000.0))
                .rewardRate(BigDecimal.valueOf(0.01))
                .build();

        BigDecimal contribution = new VariableContributionStrategy()
                .computeContribution(BigDecimal.valueOf(100), full);

        assertThat(contribution).isEqualByComparingTo(BigDecimal.valueOf(0.0));
    }
}
