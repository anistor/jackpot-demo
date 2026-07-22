package io.github.anistor.jackpot.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.github.anistor.jackpot.service.BetProcessingService;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes bets from the 'jackpot-bets' topic and hands them to the processing service.
 * Because Kafka delivers at-least-once, processing is idempotent (we dedup on bet id).
 * Retries (e.g. for optimistic locking conflicts - the DB-level safety net behind Kafka's
 * per-jackpot partition ordering) and dead-lettering after exhausting them are handled by the
 * container-level error handler configured in {@link io.github.anistor.jackpot.config.KafkaConsumerConfig},
 * not here.
 */
@Component
@RequiredArgsConstructor
public class BetConsumer {

    private final BetProcessingService processingService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        Bet bet = deserialize(payload);
        processingService.process(bet);
    }

    private Bet deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, Bet.class);
        } catch (JacksonException e) {
            throw new NonRetryableMessageException("Failed to deserialize Bet payload: " + payload, e);
        }
    }
}

