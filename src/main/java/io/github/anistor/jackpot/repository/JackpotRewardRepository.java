package io.github.anistor.jackpot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.anistor.jackpot.domain.JackpotRewardEntity;

public interface JackpotRewardRepository extends JpaRepository<JackpotRewardEntity, Long> {

    @Query("SELECT r FROM JackpotRewardEntity r WHERE r.bet.betId = :betId")
    Optional<JackpotRewardEntity> findByBetId(@Param("betId") String betId);
}
