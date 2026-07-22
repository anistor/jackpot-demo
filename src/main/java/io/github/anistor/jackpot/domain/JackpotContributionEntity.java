package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Record of a single bet's contribution to a jackpot pool (assignment section 3c).
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jackpot_contribution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JackpotContributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String betId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String jackpotId;

    @Column(nullable = false)
    private BigDecimal stakeAmount;

    @Column(nullable = false)
    private BigDecimal contributionAmount;

    @Column(nullable = false)
    private BigDecimal currentJackpotAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    @SuppressWarnings("unused")
    public JackpotContributionEntity(String betId, String userId, String jackpotId, BigDecimal stakeAmount, BigDecimal contributionAmount, BigDecimal currentJackpotAmount) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.stakeAmount = stakeAmount;
        this.contributionAmount = contributionAmount;
        this.currentJackpotAmount = currentJackpotAmount;
    }
}
