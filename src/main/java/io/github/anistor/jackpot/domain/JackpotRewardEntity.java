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
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Record of a jackpot reward paid to a winning bet (assignment section 4f).
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jackpot_reward",
        indexes = {
                @Index(name = "idx_reward_user_id", columnList = "user_id"),
                @Index(name = "idx_reward_jackpot_id", columnList = "jackpot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JackpotRewardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jackpot_reward_seq")
    @SequenceGenerator(name = "jackpot_reward_seq", sequenceName = "jackpot_reward_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true)
    private String betId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String jackpotId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    @SuppressWarnings("unused")
    public JackpotRewardEntity(String betId, String userId, String jackpotId, BigDecimal rewardAmount) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.rewardAmount = rewardAmount;
    }
}
