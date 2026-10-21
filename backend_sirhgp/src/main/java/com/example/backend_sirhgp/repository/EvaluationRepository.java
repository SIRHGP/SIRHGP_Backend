// ─── EvaluationRepository.java ────────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // Fetch-joins (évalue + critères) — retours concrets.
    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres WHERE e.entreprise.id = :entrepriseId")
    List<Evaluation> findByEntrepriseIdWithRelations(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres")
    List<Evaluation> findAllWithRelations();

    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres WHERE e.evalue.id = :evalueId")
    List<Evaluation> findByEvalueIdWithRelations(@Param("evalueId") Long evalueId);

    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres WHERE e.evaluateur.id = :evaluateurId")
    List<Evaluation> findByEvaluateurIdWithRelations(@Param("evaluateurId") Long evaluateurId);

    // Refonte 6.1 : liste bornée au périmètre départemental d'un LEAD
    // (filtre sur l'évalué, les relations sont fetch-joinées → aucun lazy).
    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres WHERE e.evalue.id IN :evalueIds")
    List<Evaluation> findByEvalueIdInWithRelations(@Param("evalueIds") List<Long> evalueIds);

    // Retour d'une seule (concret pour éviter le Optional Spring Data)
    // DISTINCT obligatoire : sans lui, le fetch-join sur la collection
    // criteres duplique la ligne dès qu'il y a > 1 critère → 500
    // (IncorrectResultSizeDataAccessException: query did not return a unique result).
    @Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.evalue LEFT JOIN FETCH e.evaluateur LEFT JOIN FETCH e.criteres WHERE e.id = :id")
    Evaluation findByIdWithRelations(@Param("id") Long id);
}