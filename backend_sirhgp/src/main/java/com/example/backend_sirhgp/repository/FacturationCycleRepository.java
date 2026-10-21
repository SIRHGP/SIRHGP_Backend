package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.FacturationCycle;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturationCycleRepository extends JpaRepository<FacturationCycle, Long> {

    Optional<FacturationCycle> findByNumeroReference(String numeroReference);
    boolean existsByNumeroReference(String numeroReference);

    List<FacturationCycle> findByEntrepriseId(Long entrepriseId, Sort sort);

    /**
     * Chargement avec relations (évite N+1 et LazyInitializationException
     * lors de la sérialisation du dossier : projet, entreprise, client,
     * proformaDocument, factureFinaleDocument, bonCommande, factureFinale).
     */
    @Query("SELECT c FROM FacturationCycle c " +
            "LEFT JOIN FETCH c.projet " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.proformaDocument " +
            "LEFT JOIN FETCH c.factureFinaleDocument " +
            "LEFT JOIN FETCH c.bonCommande " +
            "LEFT JOIN FETCH c.factureFinale " +
            "WHERE c.id = :id")
    Optional<FacturationCycle> findByIdWithRelations(@Param("id") Long id);

    /** Liste avec relations de base (évite N+1 sur la liste des dossiers). */
    @Query("SELECT DISTINCT c FROM FacturationCycle c " +
            "LEFT JOIN FETCH c.projet " +
            "LEFT JOIN FETCH c.entreprise " +
            "LEFT JOIN FETCH c.client " +
            "LEFT JOIN FETCH c.proformaDocument " +
            "LEFT JOIN FETCH c.factureFinaleDocument " +
            "LEFT JOIN FETCH c.bonCommande " +
            "LEFT JOIN FETCH c.factureFinale")
    List<FacturationCycle> findAllWithRelations(Sort sort);
}