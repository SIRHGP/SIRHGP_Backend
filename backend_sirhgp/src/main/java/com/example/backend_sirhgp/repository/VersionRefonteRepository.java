package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.VersionRefonte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Refonte 5.9 (§5) — historique des refontes 5.x. */
@Repository
public interface VersionRefonteRepository extends JpaRepository<VersionRefonte, Long> {
    Optional<VersionRefonte> findByNumero(String numero);
    List<VersionRefonte> findAllByOrderByNumeroDesc();
}