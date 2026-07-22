package io.github.anistor.jackpot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the 'jackpot-bets' topic. Multiple partitions allow bets for different jackpots be processed in parallel
 * while, thanks to the Jackpot ID message key used as routing key, all bets for one jackpot end up in a single partition
 * and are processed in the exact order they were posted.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int REPLICA_COUNT = 1;

    @Bean
    @ConditionalOnProperty(name = "app.kafka.create-topic", havingValue = "true", matchIfMissing = true)
    public NewTopic jackpotBetsTopic(@Value("${app.kafka.topic}") String topicName,
                                     @Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(REPLICA_COUNT)
                .build();
    }

    /**
     * The dead-letter topic for 'jackpot-bets', named per Spring Kafka's {@code
     * DeadLetterPublishingRecoverer} convention ({@code <topic>.DLT}). Must have at least as many
     * partitions as the source topic since the recoverer republishes to the same partition number
     * as the original record by default.
     */
    @Bean
    @ConditionalOnProperty(name = "app.kafka.create-topic", havingValue = "true", matchIfMissing = true)
    public NewTopic jackpotBetsDeadLetterTopic(@Value("${app.kafka.topic}") String topicName,
                                               @Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(topicName + ".DLT")
                .partitions(partitions)
                .replicas(REPLICA_COUNT)
                .build();
    }
}
