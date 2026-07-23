package io.github.anistor.jackpot.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Record of a single bet's contribution to a jackpot pool.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "contribution",
        indexes = {
                @Index(name = "idx_contribution_user_id", columnList = "user_id"),
                @Index(name = "idx_contribution_jackpot_id", columnList = "jackpot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JackpotContributionEntity implements Persistable<String> {

    /**
     * Shares its primary key with the processed bet it belongs to (one contribution per bet):
     * {@code bet_id} is simultaneously this row's PK and its FK to {@code processed_bet}, so the
     * business key is not duplicated across two columns.
     */
    @Id
    @Column(length = 36)
    private String betId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "bet_id", foreignKey = @ForeignKey(name = "fk_contribution_bet_id"))
    private ProcessedBetEntity bet;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jackpot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contribution_jackpot_id"))
    private JackpotEntity jackpot;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal stakeAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal contributionAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentJackpotAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @SuppressWarnings("unused")
    private Instant createdAt;

    @Builder
    @SuppressWarnings("unused")
    public JackpotContributionEntity(ProcessedBetEntity bet, String userId, JackpotEntity jackpot, BigDecimal stakeAmount, BigDecimal contributionAmount, BigDecimal currentJackpotAmount) {
        this.bet = bet;
        this.betId = bet.getBetId();
        this.userId = userId;
        this.jackpot = jackpot;
        this.stakeAmount = stakeAmount;
        this.contributionAmount = contributionAmount;
        this.currentJackpotAmount = currentJackpotAmount;
    }

    @Override
    public String getId() {
        return betId;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
