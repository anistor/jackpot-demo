package io.github.anistor.jackpot.utils;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.domain.JackpotEntity;
import io.github.anistor.jackpot.repository.JackpotRepository;
import io.github.anistor.jackpot.service.strategy.ContributionStrategy;
import io.github.anistor.jackpot.service.strategy.RewardStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds some demo jackpots on startup:
 * - one with fixed contribution and fixed reward odds,
 * - one with variable contribution and variable pool-driven reward odds
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JackpotDataInitializer implements CommandLineRunner {

    private final JackpotRepository jackpotRepository;

    @Override
    public void run(String... args) {
        if (jackpotRepository.count() == 0) {
            jackpotRepository.saveAll(List.of(
                    JackpotEntity.builder()
                            .id("JP-FIXED")
                            .initialPool(BigDecimal.valueOf(1000))
                            .contributionType(ContributionStrategy.ContributionType.FIXED)
                            .contributionRate(BigDecimal.valueOf(0.05))
                            .rewardType(RewardStrategy.RewardType.FIXED)
                            .rewardRate(BigDecimal.valueOf(0.01))
                            .build(),

                    JackpotEntity.builder()
                            .id("JP-VARIABLE")
                            .initialPool(BigDecimal.valueOf(5000))
                            .contributionType(ContributionStrategy.ContributionType.VARIABLE)
                            .contributionRate(BigDecimal.valueOf(0.1))
                            .contributionPoolLimit(BigDecimal.valueOf(100000))
                            .rewardType(RewardStrategy.RewardType.VARIABLE)
                            .rewardRate(BigDecimal.valueOf(0.001))
                            .rewardPoolLimit(BigDecimal.valueOf(50000))
                            .build()));

            log.info("Seeded jackpots: JP-FIXED, JP-VARIABLE");
        } else {
            log.info("Jackpots already seeded");
        }

    }
}
