// ─── EntrepriseRepository.java ───────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    Optional<Entreprise> findByName(String name);

    boolean existsByName(String name);

    List<Entreprise> findByStatut(Entreprise.StatutEntreprise statut);
}