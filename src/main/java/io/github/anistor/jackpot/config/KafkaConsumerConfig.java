package io.github.anistor.jackpot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import org.springframework.util.backoff.FixedBackOff;

import io.github.anistor.jackpot.messaging.NonRetryableMessageException;

/**
 * Configure dead-lettering for the bet-processing listener, BetConsumer.
 * DefaultErrorHandler is an auto-detected bean and is wired into the autoconfigured listener container factory.
 * <p>
 * A record that keeps failing after {@code app.outbox.max-process-retries} attempts is published to
 * the DLT topic instead of blocking the partition or being retried forever.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer,
                                                 @Value("${app.outbox.max-process-retries:3}") int maxProcessRetries,
                                                 @Value("${app.kafka.retry-backoff-ms:200}") long retryBackoffMs) {
        FixedBackOff backOff = new FixedBackOff(retryBackoffMs, Math.max(maxProcessRetries - 1, 0));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(NonRetryableMessageException.class);
        return errorHandler;
    }
}
