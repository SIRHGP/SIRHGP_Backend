// ─── FeuilleTempsRepository.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.FeuilleTemps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FeuilleTempsRepository extends JpaRepository<FeuilleTemps, Long> {

    // Fetch-joins (Hibernate 6 : éviter "Illegal pop()" sur proxies lazy)
    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.entreprise.id = :entrepriseId")
    List<FeuilleTemps> findByEntrepriseIdWithRelations(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache")
    List<FeuilleTemps> findAllWithRelations();

    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.employe.id = :employeId")
    List<FeuilleTemps> findByEmployeIdWithRelations(@Param("employeId") Long employeId);

    // Refonte LEAD 6.0 : périmètre départemental du chef d'équipe (feuilles des
    // employés de SES départements, tous projets confondus).
    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.employe.id IN :employeIds")
    List<FeuilleTemps> findByEmployeIdInWithRelations(@Param("employeIds") List<Long> employeIds);

    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.projet.id = :projetId")
    List<FeuilleTemps> findByProjetIdWithRelations(@Param("projetId") Long projetId);

    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.entreprise.id = :entrepriseId AND f.statut = :statut")
    List<FeuilleTemps> findByEntrepriseIdAndStatutWithRelations(@Param("entrepriseId") Long entrepriseId, @Param("statut") FeuilleTemps.StatutFeuille statut);

    @Query("SELECT DISTINCT f FROM FeuilleTemps f LEFT JOIN FETCH f.employe LEFT JOIN FETCH f.projet LEFT JOIN FETCH f.tache WHERE f.id = :id")
    FeuilleTemps findByIdWithRelations(@Param("id") Long id);

    // Total d'heures pour un employé sur une période [debut, fin]
    @Query("SELECT COALESCE(SUM(f.heures), 0) FROM FeuilleTemps f WHERE f.employe.id = :employeId AND f.date BETWEEN :debut AND :fin")
    BigDecimal sumHeuresByEmployeAndPeriode(@Param("employeId") Long employeId, @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}