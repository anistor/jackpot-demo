package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_jackpot_reward_bet_id", columnNames = "bet_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JackpotRewardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jackpot_reward_seq")
    @SequenceGenerator(name = "jackpot_reward_seq", sequenceName = "jackpot_reward_seq", allocationSize = 50)
    private Long id;

    /**
     * The processed bet this reward belongs to - at most one reward per bet.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bet_id", referencedColumnName = "bet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_jackpot_reward_bet_id"))
    private ProcessedBetEntity bet;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jackpot_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_jackpot_reward_jackpot_id"))
    private JackpotEntity jackpot;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @SuppressWarnings("unused")
    private Instant createdAt;

    @Builder
    @SuppressWarnings("unused")
    public JackpotRewardEntity(ProcessedBetEntity bet, String userId, JackpotEntity jackpot, BigDecimal rewardAmount) {
        this.bet = bet;
        this.userId = userId;
        this.jackpot = jackpot;
        this.rewardAmount = rewardAmount;
    }
}
