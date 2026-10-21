package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Quota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Refonte 5.9 (§5) — quotas plateforme (global + par entreprise). */
@Repository
public interface QuotaRepository extends JpaRepository<Quota, Long> {
    Optional<Quota> findByEntrepriseId(Long entrepriseId);
    /** Quota global par défaut (entreprise_id NULL). */
    Optional<Quota> findByEntrepriseIdIsNull();
    List<Quota> findAllByOrderByEntrepriseIdAsc();
}