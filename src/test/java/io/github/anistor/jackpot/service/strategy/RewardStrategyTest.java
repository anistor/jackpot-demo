package io.github.anistor.jackpot.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.github.anistor.jackpot.domain.JackpotEntity;

class RewardStrategyTest {

    private JackpotEntity jackpot(RewardStrategy.RewardType type, BigDecimal rate, BigDecimal limit, BigDecimal currentPool) {
        JackpotEntity j = JackpotEntity.builder()
                .id("J")
                .initialPool(BigDecimal.valueOf(1000))
                .contributionType(ContributionStrategy.ContributionType.FIXED)
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardType(type)
                .rewardRate(rate)
                .rewardPoolLimit(limit)
                .build();
        j.addContributionToPool(currentPool.subtract(j.getInitialPool()));
        return j;
    }

    @Test
    void fixedRewardChanceIsConstant() {
        JackpotEntity small = jackpot(RewardStrategy.RewardType.FIXED, BigDecimal.valueOf(0.25), null, BigDecimal.valueOf(1000));
        JackpotEntity large = jackpot(RewardStrategy.RewardType.FIXED, BigDecimal.valueOf(0.25), null, BigDecimal.valueOf(999999));

        FixedRewardStrategy strategy = new FixedRewardStrategy();

        assertThat(strategy.computeChance(small)).isEqualTo(0.25);
        assertThat(strategy.computeChance(large)).isEqualTo(0.25);
    }

    @Test
    void variableRewardChanceRisesWithPool() {
        VariableRewardStrategy strategy = new VariableRewardStrategy();
        JackpotEntity low = jackpot(RewardStrategy.RewardType.VARIABLE, BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000), BigDecimal.valueOf(1000));
        JackpotEntity high = jackpot(RewardStrategy.RewardType.VARIABLE, BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000), BigDecimal.valueOf(5000));

        assertThat(strategy.computeChance(high)).isGreaterThan(strategy.computeChance(low));
    }

    @Test
    void variableRewardChanceCapsAtOneBeyondLimit() {
        VariableRewardStrategy strategy = new VariableRewardStrategy();
        JackpotEntity full = jackpot(RewardStrategy.RewardType.VARIABLE, BigDecimal.valueOf(0.01), BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));

        assertThat(strategy.computeChance(full)).isEqualTo(1);
    }
}
