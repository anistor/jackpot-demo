package io.github.anistor.jackpot.service.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Resolves the {@link ContributionStrategy} to apply for a jackpot's configured type.
 * All strategy beans are auto-discovered.
 */
@Component
public class ContributionStrategyFactory {

    private final Map<ContributionStrategy.ContributionType, ContributionStrategy> strategies = new EnumMap<>(ContributionStrategy.ContributionType.class);

    public ContributionStrategyFactory(List<ContributionStrategy> strategyBeans) {
        for (ContributionStrategy strategy : strategyBeans) {
            ContributionStrategy existing = strategies.put(strategy.type(), strategy);
            if (existing != null) {
                throw new IllegalStateException("Duplicate contribution strategy for type " + strategy.type()
                        + ". We already have " + existing.getClass().getName()
                        + " and now we are trying to register " + strategy.getClass().getName());
            }
        }
    }

    public ContributionStrategy forType(ContributionStrategy.ContributionType type) {
        ContributionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No contribution strategy registered for " + type);
        }
        return strategy;
    }
}
