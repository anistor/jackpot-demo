package io.github.anistor.jackpot.service.strategy;

import java.math.BigDecimal;

import io.github.anistor.jackpot.domain.JackpotConfiguration;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
final class TestJackpotConfiguration implements JackpotConfiguration {

    private final BigDecimal initialPool;
    private final BigDecimal currentPool;
    private final BigDecimal contributionRate;
    private final BigDecimal contributionPoolLimit;
    private final BigDecimal rewardRate;
    private final BigDecimal rewardPoolLimit;
}
