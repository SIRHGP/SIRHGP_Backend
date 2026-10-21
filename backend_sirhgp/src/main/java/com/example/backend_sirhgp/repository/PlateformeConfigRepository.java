package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.PlateformeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Refonte 5.9 (§5) — config globale de la plateforme. */
@Repository
public interface PlateformeConfigRepository extends JpaRepository<PlateformeConfig, Long> {
    Optional<PlateformeConfig> findByCle(String cle);
    boolean existsByCle(String cle);
}