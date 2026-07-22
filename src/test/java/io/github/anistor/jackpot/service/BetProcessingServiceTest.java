package io.github.anistor.jackpot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.anistor.jackpot.domain.JackpotContributionEntity;
import io.github.anistor.jackpot.domain.JackpotEntity;
import io.github.anistor.jackpot.domain.JackpotRewardEntity;
import io.github.anistor.jackpot.domain.ProcessedBetEntity;
import io.github.anistor.jackpot.messaging.Bet;
import io.github.anistor.jackpot.repository.JackpotContributionRepository;
import io.github.anistor.jackpot.repository.JackpotRepository;
import io.github.anistor.jackpot.repository.JackpotRewardRepository;
import io.github.anistor.jackpot.repository.ProcessedBetRepository;
import io.github.anistor.jackpot.service.strategy.ContributionStrategy;
import io.github.anistor.jackpot.service.strategy.RewardStrategy;

import com.github.f4b6a3.uuid.UuidCreator;

@SpringBootTest
class BetProcessingServiceTest {

    @Autowired
    private BetProcessingService processingService;

    @Autowired
    private JackpotRepository jackpotRepository;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Autowired
    private JackpotRewardRepository rewardRepository;

    @Autowired
    private ProcessedBetRepository processedBetRepository;

    private JackpotEntity alwaysLoses(String id) {
        return jackpotRepository.save(JackpotEntity.builder()
                .id(id)
                .initialPool(BigDecimal.valueOf(1000))
                .contributionType(ContributionStrategy.ContributionType.FIXED)
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardType(RewardStrategy.RewardType.FIXED)
                .rewardRate(BigDecimal.ZERO)
                .build());
    }

    private JackpotEntity alwaysWins(String id) {
        return jackpotRepository.save(JackpotEntity.builder()
                .id(id)
                .initialPool(BigDecimal.valueOf(1000))
                .contributionType(ContributionStrategy.ContributionType.FIXED)
                .contributionRate(BigDecimal.valueOf(0.05))
                .rewardType(RewardStrategy.RewardType.FIXED)
                .rewardRate(BigDecimal.ONE)
                .build());
    }

    @Test
    void appliesContributionAndRecordsLoss() {
        JackpotEntity jackpot = alwaysLoses("JP-LOSE-" + randomUUID());
        Bet bet = new Bet(randomUUID(), "user-1", jackpot.getId(), BigDecimal.valueOf(200));

        processingService.process(bet);

        ProcessedBetEntity outcome = processedBetRepository.findByBetId(bet.betId()).orElseThrow();
        assertThat(outcome.isWon()).isFalse();
        assertThat(outcome.getRewardAmount()).isNull();
        assertThat(outcome.getProcessedAt()).isNotNull();

        JackpotContributionEntity contribution = contributionRepository.findByBetId(bet.betId()).orElseThrow();
        assertThat(contribution.getContributionAmount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(contribution.getCreatedAt()).isNotNull();
        assertThat(jackpotRepository.findById(jackpot.getId()).orElseThrow().getCurrentPool()).isEqualByComparingTo(BigDecimal.valueOf(1010));
    }

    @Test
    void paysOutAndResetsPoolOnWin() {
        JackpotEntity jackpot = alwaysWins("JP-WIN-" + randomUUID());
        Bet bet = new Bet(randomUUID(), "user-2", jackpot.getId(), BigDecimal.valueOf(200));

        processingService.process(bet);

        ProcessedBetEntity outcome = processedBetRepository.findByBetId(bet.betId()).orElseThrow();
        assertThat(outcome.isWon()).isTrue();
        assertThat(outcome.getRewardAmount()).isEqualByComparingTo(BigDecimal.valueOf(1010));
        assertThat(outcome.getProcessedAt()).isNotNull();

        JackpotRewardEntity reward = rewardRepository.findByBetId(bet.betId()).orElseThrow();
        assertThat(reward.getRewardAmount()).isEqualByComparingTo(BigDecimal.valueOf(1010));
        assertThat(reward.getCreatedAt()).isNotNull();
        assertThat(jackpotRepository.findById(jackpot.getId()).orElseThrow().getCurrentPool()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void skipsAlreadyProcessedBet() {
        JackpotEntity jackpot = alwaysLoses("JP-DEDUP-" + randomUUID());
        Bet bet = new Bet(randomUUID(), "user-3", jackpot.getId(), BigDecimal.valueOf(200));

        processingService.process(bet);
        processingService.process(bet);

        JackpotContributionEntity contribution = contributionRepository.findByBetId(bet.betId()).orElseThrow();
        assertThat(contribution.getCreatedAt()).isNotNull();
        assertThat(jackpotRepository.findById(jackpot.getId()).orElseThrow().getCurrentPool()).isEqualByComparingTo(BigDecimal.valueOf(1010));
    }

    private static String randomUUID() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }
}
