package io.github.anistor.jackpot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.anistor.jackpot.domain.OutboxEventEntity;
import io.github.anistor.jackpot.domain.ProcessedBetEntity;
import io.github.anistor.jackpot.repository.OutboxEventRepository;
import io.github.anistor.jackpot.repository.ProcessedBetRepository;

import com.github.f4b6a3.uuid.UuidCreator;

@SpringBootTest
class BetServiceTest {

    @Autowired
    private BetService betService;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private ProcessedBetRepository processedBetRepository;

    @Test
    void placeBetGeneratesIdAndPersistsPendingOutboxEvent() {
        String betId = betService.placeBet(Optional.empty(), "user-1", "JP-FIXED", BigDecimal.valueOf(200));

        assertThat(betId).isNotBlank();

        OutboxEventEntity event = outboxRepository.findById(betId).orElseThrow();
        assertThat(event.getRoutingKey()).isEqualTo("JP-FIXED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventEntity.Status.PENDING);
        assertThat(event.getPayload()).contains("user-1").contains("JP-FIXED");
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void placeBetUsesCallerProvidedBetId() {
        String requestedBetId = randomID();

        String betId = betService.placeBet(Optional.of(requestedBetId), "user-2", "JP-FIXED", BigDecimal.valueOf(50));

        assertThat(betId).isEqualTo(requestedBetId);
        assertThat(outboxRepository.findById(requestedBetId)).isPresent();
    }

    @Test
    void placeBetThrowsDuplicateBetExceptionOnRepeatedBetId() {
        String betId = randomID();

        betService.placeBet(Optional.of(betId), "user-3", "JP-FIXED", BigDecimal.valueOf(100));

        assertThatThrownBy(() -> betService.placeBet(Optional.of(betId), "user-3", "JP-FIXED", BigDecimal.valueOf(100)))
                .isInstanceOf(DuplicateBetException.class);
    }

    @Test
    void getBetOutcomeReturnsNotFoundForUnknownBet() {
        BetOutcome outcome = betService.getBetOutcome("does-not-exist");

        assertThat(outcome.status()).isEqualTo(BetOutcome.Status.NOT_FOUND);
    }

    @Test
    void getBetOutcomeReturnsPendingForPlacedButUnprocessedBet() {
        String betId = betService.placeBet(Optional.empty(), "user-4", "JP-FIXED", BigDecimal.valueOf(75));

        BetOutcome outcome = betService.getBetOutcome(betId);

        assertThat(outcome.status()).isEqualTo(BetOutcome.Status.PENDING);
        assertThat(outcome.betId()).isEqualTo(betId);
    }

    @Test
    void getBetOutcomeReturnsWonForProcessedWinningBet() {
        String betId = randomID();
        processedBetRepository.save(ProcessedBetEntity.builder()
                .betId(betId)
                .userId("user-5")
                .jackpotId("JP-FIXED")
                .status(ProcessedBetEntity.Status.WON)
                .rewardAmount(BigDecimal.valueOf(500))
                .build());

        BetOutcome outcome = betService.getBetOutcome(betId);

        assertThat(outcome.status()).isEqualTo(BetOutcome.Status.WON);
        assertThat(outcome.jackpotId()).isEqualTo("JP-FIXED");
        assertThat(outcome.rewardAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void getBetOutcomeReturnsLostForProcessedLosingBet() {
        String betId = randomID();
        processedBetRepository.save(ProcessedBetEntity.builder()
                .betId(betId)
                .userId("user-6")
                .jackpotId("JP-VARIABLE")
                .status(ProcessedBetEntity.Status.LOST)
                .build());

        BetOutcome outcome = betService.getBetOutcome(betId);

        assertThat(outcome.status()).isEqualTo(BetOutcome.Status.LOST);
        assertThat(outcome.rewardAmount()).isNull();
    }

    @Test
    void getBetOutcomeReturnsErrorForProcessedErroredBet() {
        String betId = randomID();
        processedBetRepository.save(ProcessedBetEntity.builder()
                .betId(betId)
                .userId("user-7")
                .jackpotId("JP-FIXED")
                .status(ProcessedBetEntity.Status.ERROR)
                .errorMessage("boom")
                .build());

        BetOutcome outcome = betService.getBetOutcome(betId);

        assertThat(outcome.status()).isEqualTo(BetOutcome.Status.ERROR);
    }

    private static String randomID() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }
}
