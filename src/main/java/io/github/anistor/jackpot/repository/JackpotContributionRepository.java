package io.github.anistor.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.JackpotContributionEntity;

public interface JackpotContributionRepository extends JpaRepository<JackpotContributionEntity, String> {
}
