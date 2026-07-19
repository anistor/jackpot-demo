package io.github.anistor.jackpot.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.service.BetProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes bets from the 'jackpot-bets' topic and hands them to the processing service.
 * Because Kafka delivers at-least-once, processing is idempotent (we dedup on bet id),
 * optimistic locking failures are retried a few times - the DB-level safety net behind Kafka's
 * per-jackpot partition ordering.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BetConsumer {

    @Value("${app.outbox.max-process-retries:3}")
    private final int maxProcessRetries;

    private final BetProcessingService processingService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        Bet bet = deserialize(payload);

        int remainingAttempts = maxProcessRetries;
        while (true) {
            try {
                processingService.process(bet);
                break;
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict processing bet {} (attempt {}/{})",
                        bet.betId(), maxProcessRetries - remainingAttempts + 1, maxProcessRetries);
                if (--remainingAttempts <= 0) {
                    throw e;
                }
            }
        }
    }

    private Bet deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, Bet.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize Bet payload: " + payload, e);
        }
    }
}
