package io.github.anistor.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.ProcessedBetEntity;

public interface ProcessedBetRepository extends JpaRepository<ProcessedBetEntity, String> {
}
