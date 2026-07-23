package io.github.anistor.jackpot.domain;

import java.time.Instant;

import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.type.json.JsonType;

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

// TODO Consider making Outbox generic (no longer Bet related) and introduce an event type discriminator for that purpose and also a way to map it to a topic name

/**
 * Transactional outbox row. A bet is persisted here as {@code PENDING} in the same transaction
 * that acknowledges it, guaranteeing it is never lost even if publishing to Kafka fails. A
 * scheduled publisher retries sending and flips the row to {@code SENT} once Kafka confirms.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_event_created_at", columnList = "created_at"),
                @Index(name = "idx_outbox_event_status_created_at", columnList = "status, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventEntity implements Persistable<String> {

    public enum Status {
        PENDING,
        SENT,
        FAILED,
    }

    @Id
    @Column(length = 36)
    private String idempotencyKey;

    /**
     * The key used for routing to the appropriate partition. This is not generally unique.
     */
    @Column(nullable = false, length = 36)
    private String routingKey;

    /**
     * Stored as a native JSON column (via hypersistence-utils' {@link JsonType}) rather than a plain
     * CLOB/text so the database validates the payload shape and it stays queryable/indexable if needed later.
     */
    @Type(JsonType.class)
    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @SuppressWarnings("unused")
    private Instant createdAt;

    @Column
    private Instant sentAt;

    @Builder
    @SuppressWarnings("unused")
    public OutboxEventEntity(String idempotencyKey, String routingKey, String payload) {
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("idempotencyKey must not be null");
        }
        if (routingKey == null) {
            throw new IllegalArgumentException("routingKey must not be null");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        this.idempotencyKey = idempotencyKey;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = Status.PENDING;
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void markSent(Instant sentAt) {
        this.status = Status.SENT;
        this.sentAt = sentAt;
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }

    @Override
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
