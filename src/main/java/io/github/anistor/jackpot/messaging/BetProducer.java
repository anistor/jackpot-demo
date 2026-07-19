package io.github.anistor.jackpot.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Publishes bet payloads to the 'jackpot-bets' topic. The message key is the Jackpot ID
 * so all bets for the same jackpot land in the same partition and are processed in order by a
 * single consumer thread thus serializing writes to that jackpot's pool. The send blocks
 * until Kafka acknowledges, so the outbox publisher only marks a row sent once it is confirmed.
 */
@Component
@RequiredArgsConstructor
public class BetProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic}")
    private final String topic;

    public void send(String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaPublishException("Failed to publish to " + topic, e);
        }
    }

    /**
     * Signals that a Kafka publish did not complete; the outbox row stays pending for retry.
     */
    public static class KafkaPublishException extends RuntimeException {
        public KafkaPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
