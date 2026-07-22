package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;

import io.github.anistor.jackpot.service.strategy.ContributionStrategy;
import io.github.anistor.jackpot.service.strategy.RewardStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A shared jackpot pool. Configured with a contribution strategy (how much of each stake is
 * added to the pool) and a reward strategy (the odds a bet wins the pool). Uses optimistic
 * locking ({@link Version}) as an extra DB-level safety net against concurrent pool updates - which should not
 * normally happen - this is ensured by our partitioning scheme.
 */
@Entity
@Table(name = "jackpot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JackpotEntity implements JackpotConfiguration {

    @Id
    private String id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal initialPool;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPool;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionStrategy.ContributionType contributionType;

    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal contributionRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal contributionPoolLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardStrategy.RewardType rewardType;

    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal rewardRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal rewardPoolLimit;

    @Version
    private long version;

    @Builder
    public JackpotEntity(String id,
                         BigDecimal initialPool,
                         ContributionStrategy.ContributionType contributionType,
                         BigDecimal contributionRate,
                         BigDecimal contributionPoolLimit,
                         RewardStrategy.RewardType rewardType,
                         BigDecimal rewardRate,
                         BigDecimal rewardPoolLimit) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (initialPool == null) {
            throw new IllegalArgumentException("initialPool must not be null");
        }
        if (initialPool.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("initialPool must be positive");
        }
        if (contributionType == null) {
            throw new IllegalArgumentException("contributionType must not be null");
        }
        if (contributionRate == null) {
            throw new IllegalArgumentException("contributionRate must not be null");
        }
        if (contributionRate.compareTo(BigDecimal.ZERO) < 0 || contributionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("contributionRate must be between 0 and 1");
        }
        if (contributionPoolLimit != null && contributionPoolLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("contributionPoolLimit must be positive when present");
        }
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType must not be null");
        }
        if (rewardRate == null) {
            throw new IllegalArgumentException("rewardRate must not be null");
        }
        if (rewardRate.compareTo(BigDecimal.ZERO) < 0 || rewardRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rewardRate must be between 0 and 1");
        }
        if (rewardPoolLimit != null && rewardPoolLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("rewardPoolLimit must be positive when present");
        }

        this.id = id;
        this.initialPool = initialPool;
        this.currentPool = initialPool;
        this.contributionType = contributionType;
        this.contributionRate = contributionRate;
        this.contributionPoolLimit = contributionPoolLimit;
        this.rewardType = rewardType;
        this.rewardRate = rewardRate;
        this.rewardPoolLimit = rewardPoolLimit;
    }

    public void addContributionToPool(BigDecimal amount) {
        this.currentPool = this.currentPool.add(amount);
    }

    public void resetPool() {
        this.currentPool = this.initialPool;
    }
}
