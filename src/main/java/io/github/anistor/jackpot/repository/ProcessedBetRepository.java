package io.github.anistor.jackpot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.ProcessedBetEntity;

public interface ProcessedBetRepository extends JpaRepository<ProcessedBetEntity, Long> {

    Optional<ProcessedBetEntity> findByBetId(String betId);

    boolean existsByBetId(String betId);
}

