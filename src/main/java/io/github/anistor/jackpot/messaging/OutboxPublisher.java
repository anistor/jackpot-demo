package io.github.anistor.jackpot.messaging;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.anistor.jackpot.domain.OutboxEventEntity;
import io.github.anistor.jackpot.repository.OutboxEventRepository;

import jakarta.validation.ClockProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//TODO This outbox publisher is a simple fixed-delay poll, hopefully good enough for a demo.
// Worth knowing that if we plan to scale out horizontally, two instances' schedulers could race and pick up
// the same PENDING rows so we'd get duplicate Kafka publishes (wasteful, but effectively harmless since consumers dedupe by betId).

/**
 * Background poller that drives the transactional outbox. On each pass it picks up pending bet
 * rows and attempts to publish them to Kafka, marking each {@code SENT} only once the broker
 * confirms. A failed publishing leaves the row pending so it is retried on the next pass -
 * nothing is lost even if Kafka is temporarily unavailable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    @Value("${app.outbox.enabled:true}")
    private final boolean enabled;

    @Value("${app.outbox.max-send-retries:3}")
    private final int maxSendRetries;

    @Value("${app.outbox.batch-size:50}")
    private final int batchSize;

    private final ClockProvider clockProvider;

    private final OutboxEventRepository outboxRepository;

    private final BetProducer betProducer;

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    @Transactional
    public void publishPending() {
        if (!enabled) {
            return;
        }

        List<OutboxEventEntity> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventEntity.Status.PENDING, Limit.of(batchSize));
        for (OutboxEventEntity event : pending) {
            event.recordAttempt();

            try {
                betProducer.send(event.getRoutingKey(), event.getPayload());
                event.markSent(clockProvider.getClock().instant());
                log.debug("Published bet {} to Kafka", event.getIdempotencyKey());
            } catch (Exception e) {
                if (event.getAttempts() >= maxSendRetries) {
                    event.markFailed();
                }
                log.error("Failed to publish bet '{}' (attempts:{}, retry:{}) : {}",
                        event.getIdempotencyKey(),
                        event.getAttempts(),
                        event.getStatus() == OutboxEventEntity.Status.FAILED ? 'N' : 'Y',
                        e.getMessage());
            }
        }

        // All mutated rows are saved together in one JDBC batch (see hibernate.jdbc.batch_size /
        // hibernate.order_updates in application.yaml) instead of one round-trip per row. Since
        // this whole method runs in a single transaction, a failure here rolls back every row in
        // the batch - including ones already successfully published to Kafka - so they'd be
        // retried (and re-published) on the next poll. Harmless since consumers dedupe by betId,
        // but worth knowing.
        outboxRepository.saveAll(pending);
    }
}
