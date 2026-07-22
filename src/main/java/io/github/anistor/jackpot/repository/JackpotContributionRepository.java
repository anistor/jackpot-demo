package io.github.anistor.jackpot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.anistor.jackpot.domain.JackpotContributionEntity;

public interface JackpotContributionRepository extends JpaRepository<JackpotContributionEntity, Long> {

    @Query("SELECT c FROM JackpotContributionEntity c WHERE c.bet.betId = :betId")
    Optional<JackpotContributionEntity> findByBetId(@Param("betId") String betId);
}
