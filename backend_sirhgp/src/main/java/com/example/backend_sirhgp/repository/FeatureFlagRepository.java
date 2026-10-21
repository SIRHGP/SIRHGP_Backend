package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Refonte 5.9 (§5) — feature flags globaux. */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByCle(String cle);
    boolean existsByCle(String cle);
}