package io.github.anistor.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.JackpotRewardEntity;

public interface JackpotRewardRepository extends JpaRepository<JackpotRewardEntity, String> {
}
