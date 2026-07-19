package io.github.anistor.jackpot.service.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Resolves the {@link RewardStrategy} to apply for a jackpot's configured type.
 * All strategy beans are auto-discovered.
 */
@Component
public class RewardStrategyFactory {

    private final Map<RewardStrategy.RewardType, RewardStrategy> strategies = new EnumMap<>(RewardStrategy.RewardType.class);

    public RewardStrategyFactory(List<RewardStrategy> strategyBeans) {
        for (RewardStrategy strategy : strategyBeans) {
            RewardStrategy existing = strategies.put(strategy.type(), strategy);
            if (existing != null) {
                throw new IllegalStateException("Duplicate reward strategy for type " + strategy.type()
                        + ". We already have " + existing.getClass().getName()
                        + " and now we are trying to register " + strategy.getClass().getName());
            }
        }
    }

    public RewardStrategy forType(RewardStrategy.RewardType type) {
        RewardStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No reward strategy registered for " + type);
        }
        return strategy;
    }
}
