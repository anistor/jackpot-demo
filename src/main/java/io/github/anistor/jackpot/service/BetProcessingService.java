package io.github.anistor.jackpot.service;

import java.math.BigDecimal;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import io.github.anistor.jackpot.service.strategy.ContributionStrategyFactory;
import io.github.anistor.jackpot.service.strategy.RewardStrategy;
import io.github.anistor.jackpot.service.strategy.RewardStrategyFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The core of the Kafka consumer's work. For each bet it: deduplicates on bet id, applies the
 * jackpot's contribution strategy and updates the pool, then it rolls the reward using
 * the pool state that bet it just created. On a win pays out the pool and resets it, and
 * records the outcome, so if future redeliveries happen we are still gracefully idempotent.
 * <p>
 * Runs in a single transaction. The jackpot row carries a {@code @Version}, so an eventual concurrent
 * update triggers an optimistic-lock failure the caller can retry - a DB-level safety net
 * behind Kafka's per-jackpot partition ordering. Such locking failures should be very rare and never self-inflicted.
 * We can only collide with manual DB changes or other changes not under our control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BetProcessingService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final JackpotRewardRepository rewardRepository;
    private final ProcessedBetRepository processedBetRepository;

    private final ContributionStrategyFactory contributionStrategyFactory;
    private final RewardStrategyFactory rewardStrategyFactory;

    private final RandomGenerator random;

    @Transactional
    public void process(Bet bet) {
        if (processedBetRepository.existsByBetId(bet.betId())) {
            log.debug("Bet {} already processed, skipping (idempotent)", bet.betId());
            return;
        }

        JackpotEntity jackpot = jackpotRepository.findById(bet.jackpotId()).orElse(null);
        if (jackpot == null) {
            log.error("Jackpot '{}' not found for bet '{}'", bet.jackpotId(), bet.betId());
            ProcessedBetEntity processedBet = ProcessedBetEntity.builder()
                    .betId(bet.betId())
                    .userId(bet.userId())
                    .jackpotId(bet.jackpotId())
                    .status(ProcessedBetEntity.Status.ERROR)
                    .errorMessage("Jackpot not found")
                    .build();
            processedBetRepository.save(processedBet);
            return;
        }

        ContributionStrategy contributionStrategy = contributionStrategyFactory.forType(jackpot.getContributionType());
        BigDecimal contributionAmount = contributionStrategy
                .computeContribution(bet.betAmount(), jackpot);
        jackpot.addContributionToPool(contributionAmount);

        JackpotContributionEntity contribution = JackpotContributionEntity.builder()
                .betId(bet.betId())
                .userId(bet.userId())
                .jackpotId(bet.jackpotId())
                .stakeAmount(bet.betAmount())
                .contributionAmount(contributionAmount)
                .currentJackpotAmount(jackpot.getCurrentPool())
                .build();
        contributionRepository.save(contribution);

        boolean won = evaluateBet(jackpot);

        BigDecimal rewardAmount = null;
        if (won) {
            rewardAmount = jackpot.getCurrentPool();
            JackpotRewardEntity reward = JackpotRewardEntity.builder()
                    .betId(bet.betId())
                    .userId(bet.userId())
                    .jackpotId(bet.jackpotId())
                    .rewardAmount(rewardAmount)
                    .build();
            rewardRepository.save(reward);
            jackpot.resetPool();
            log.info("Bet {} WON jackpot {} for {}", bet.betId(), bet.jackpotId(), rewardAmount);
        } else {
            log.debug("Bet {} did not win jackpot {}", bet.betId(), bet.jackpotId());
        }

        jackpotRepository.save(jackpot);

        ProcessedBetEntity processedBet = ProcessedBetEntity.builder()
                .betId(bet.betId())
                .userId(bet.userId())
                .jackpotId(bet.jackpotId())
                .status(won ? ProcessedBetEntity.Status.WON : ProcessedBetEntity.Status.LOST)
                .rewardAmount(rewardAmount)
                .build();
        processedBetRepository.save(processedBet);
    }

    /**
     * Rolls the dice.
     *
     * @return true if the bet won the jackpot, false otherwise
     */
    private boolean evaluateBet(JackpotEntity jackpot) {
        RewardStrategy rewardStrategy = rewardStrategyFactory.forType(jackpot.getRewardType());
        double chance = rewardStrategy.computeChance(jackpot);
        double rand = random.nextDouble();
        return rand < chance; // textbook way to "roll the dice" with given probability
    }
}
