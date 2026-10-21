package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.FeatureFlagOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Refonte 5.9 (§5) — overrides de feature flags par entreprise. */
@Repository
public interface FeatureFlagOverrideRepository extends JpaRepository<FeatureFlagOverride, Long> {
    List<FeatureFlagOverride> findByFlagId(Long flagId);
    Optional<FeatureFlagOverride> findByFlagIdAndEntrepriseId(Long flagId, Long entrepriseId);
}