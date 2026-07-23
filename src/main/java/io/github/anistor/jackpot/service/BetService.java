package io.github.anistor.jackpot.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.anistor.jackpot.domain.OutboxEventEntity;
import io.github.anistor.jackpot.domain.ProcessedBetEntity;
import io.github.anistor.jackpot.messaging.Bet;
import io.github.anistor.jackpot.repository.OutboxEventRepository;
import io.github.anistor.jackpot.repository.ProcessedBetRepository;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Entry point for placing bets. Persists the bet to the transactional outbox so it is never
 * lost, and exposes the already-decided outcome for the reward evaluation endpoint.
 */
@Service
@RequiredArgsConstructor
public class BetService {

    private final OutboxEventRepository outboxRepository;

    private final ProcessedBetRepository processedBetRepository;

    private final ObjectMapper objectMapper;

    /**
     * Records a bet as a pending outbox event in a single transaction. The wallet has already
     * been debited upstream, so the bet record is the proof money is owed to a jackpot: writing
     * it here (rather than just publishing to Kafka directly) guarantees it survives a broker outage.
     */
    @Transactional
    public String placeBet(String userId, String jackpotId, BigDecimal amount) {
        // use time-ordered UUID v7 because they index better in databases
        String betId = UuidCreator.getTimeOrderedEpoch().toString();

        Bet bet = new Bet(betId, userId, jackpotId, amount);
        String payload = serialize(bet);

        outboxRepository.save(OutboxEventEntity.builder()
                .idempotencyKey(betId)
                .routingKey(jackpotId)
                .payload(payload)
                .build());

        return betId;
    }

    @Transactional(readOnly = true)
    public BetOutcome getBetOutcome(String betId) {
        return processedBetRepository.findById(betId)
                .map(processedBet -> switch (processedBet.getStatus()) {
                    case ProcessedBetEntity.Status.WON ->
                            BetOutcome.won(processedBet.getBetId(), processedBet.getJackpotId(), processedBet.getRewardAmount());
                    case ProcessedBetEntity.Status.LOST ->
                            BetOutcome.lost(processedBet.getBetId(), processedBet.getJackpotId());
                    case ProcessedBetEntity.Status.ERROR ->
                            BetOutcome.error(processedBet.getBetId(), processedBet.getJackpotId());
                })
                .orElseGet(() -> outboxRepository.existsById(betId)
                        ? BetOutcome.pending(betId)
                        : BetOutcome.notFound(betId));
    }

    private String serialize(Bet bet) {
        try {
            return objectMapper.writeValueAsString(bet);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize Bet " + bet.betId(), e);
        }
    }
}
