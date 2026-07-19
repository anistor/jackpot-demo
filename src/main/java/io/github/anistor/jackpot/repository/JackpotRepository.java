package io.github.anistor.jackpot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.anistor.jackpot.domain.JackpotEntity;

public interface JackpotRepository extends JpaRepository<JackpotEntity, String> {
}
