package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Marks a bet as already processed by the consumer. Serves two purposes:
 * - Idempotency key - the consumer skips a bet whose id already exists here (Kafka delivers at-least-once).
 * - Source of the already-decided outcome returned by the reward evaluation endpoint.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "processed_bet",
        indexes = {
                @Index(name = "idx_processed_bet_user_id", columnList = "user_id"),
                @Index(name = "idx_processed_bet_jackpot_id", columnList = "jackpot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedBetEntity implements Persistable<String> {

    public enum Status {
        WON,
        LOST,
        ERROR,
    }

    @Id
    @Column(name = "bet_id", length = 36)
    private String betId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 36)
    private String jackpotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    @Column(length = 256)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @SuppressWarnings("unused")
    private Instant processedAt;

    @Builder
    public ProcessedBetEntity(String betId, String userId, String jackpotId, Status status, String errorMessage, BigDecimal rewardAmount) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.rewardAmount = rewardAmount;
    }

    public boolean isWon() {
        return status == Status.WON;
    }

    @Override
    public String getId() {
        return betId;
    }

    @Override
    public boolean isNew() {
        return processedAt == null;
    }
}
