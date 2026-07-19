package io.github.anistor.jackpot.config;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.validation.ClockProvider;

/**
 * Some components defined centrally, so we can easily override them in tests to simulate reproducible betting conditions.
 */
@Configuration
@EnableScheduling
@EnableJpaAuditing
public class AppConfig {

    @Bean
    public RandomGenerator randomGenerator() {
        return new SecureRandom();
    }

    @Bean
    public ClockProvider clockProvider() {
        return Clock::systemDefaultZone;
    }
}
