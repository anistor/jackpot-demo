package io.github.anistor.jackpot.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

class RewardStrategyTest {

    @Test
    void fixedRewardChanceIsConstant() {
        JackpotConfiguration small = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(1000))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.25))
                .build();
        JackpotConfiguration large = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(999999))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.25))
                .build();

        FixedRewardStrategy strategy = new FixedRewardStrategy();

        assertThat(strategy.computeChance(small)).isEqualTo(0.25);
        assertThat(strategy.computeChance(large)).isEqualTo(0.25);
    }

    @Test
    void variableRewardChanceRisesWithPool() {
        VariableRewardStrategy strategy = new VariableRewardStrategy();
        JackpotConfiguration low = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(1000))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.01))
                .rewardPoolLimit(BigDecimal.valueOf(10000))
                .build();
        JackpotConfiguration high = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(5000))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.01))
                .rewardPoolLimit(BigDecimal.valueOf(10000))
                .build();

        assertThat(strategy.computeChance(high)).isGreaterThan(strategy.computeChance(low));
    }

    @Test
    void variableRewardChanceCapsAtOneBeyondLimit() {
        VariableRewardStrategy strategy = new VariableRewardStrategy();
        JackpotConfiguration full = TestJackpotConfiguration.builder()
                .initialPool(BigDecimal.valueOf(1000))
                .currentPool(BigDecimal.valueOf(20000))
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardRate(BigDecimal.valueOf(0.01))
                .rewardPoolLimit(BigDecimal.valueOf(10000))
                .build();

        assertThat(strategy.computeChance(full)).isEqualTo(1);
    }
}
