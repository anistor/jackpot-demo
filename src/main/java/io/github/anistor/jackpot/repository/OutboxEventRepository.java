package io.github.anistor.jackpot.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.OutboxEventEntity;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventEntity.Status status, Limit limit);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
