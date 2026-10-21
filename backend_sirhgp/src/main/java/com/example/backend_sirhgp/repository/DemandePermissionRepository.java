package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.DemandePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Refonte 5.6 — Demandes de permission (Timesheet).
 * Les requêtes chargent le demandeur avec fetch-join (anti-N+1) ; le valideur
 * est stocké en colonnes plates (id/nom), donc aucun lazy-load à craindre.
 */
@Repository
public interface DemandePermissionRepository extends JpaRepository<DemandePermission, Long> {

    /**
     * File « À valider » d'un MANAGER/ADMIN (scope entreprise) :
     * demandes orphelines (valideurId null) en attente.
     */
    @Query("SELECT p FROM DemandePermission p LEFT JOIN FETCH p.demandeur " +
            "WHERE p.statut = 'EN_ATTENTE' AND p.valideurId IS NULL AND p.entreprise.id = :entrepriseId ORDER BY p.dateCreation ASC")
    List<DemandePermission> findOrphelinesByEntreprise(@Param("entrepriseId") Long entrepriseId);

    /**
     * Garde-fou anti-chevauchement : une demande EN_ATTENTE ou APPROUVEE du
     * même employé recouvrant [debut, fin] bloque la nouvelle demande.
     */
    @Query("SELECT COUNT(p) FROM DemandePermission p WHERE p.demandeur.id = :demandeurId " +
            "AND p.statut IN ('EN_ATTENTE', 'APPROUVEE') " +
            "AND p.dateDebut <= :fin AND p.dateFin >= :debut")
    long countChevauchements(@Param("demandeurId") Long demandeurId,
                             @Param("debut") java.time.LocalDate debut,
                             @Param("fin") java.time.LocalDate fin);

    /** Mes demandes d'un employé (avec demandeur fetch-join). */
    @Query("SELECT p FROM DemandePermission p LEFT JOIN FETCH p.demandeur " +
            "WHERE p.demandeur.id = :demandeurId ORDER BY p.dateCreation DESC")
    List<DemandePermission> findByDemandeurIdWithDemandeur(@Param("demandeurId") Long demandeurId);

    /** Demandes EN_ATTENTE où je suis le valideur snapshoté. */
    @Query("SELECT p FROM DemandePermission p LEFT JOIN FETCH p.demandeur " +
            "WHERE p.valideurId = :valideurId AND p.statut = 'EN_ATTENTE' ORDER BY p.dateCreation ASC")
    List<DemandePermission> findEnAttenteByValideur(@Param("valideurId") Long valideurId);

    /** Toutes les demandes orphelines (valideurId null) en attente. */
    @Query("SELECT p FROM DemandePermission p LEFT JOIN FETCH p.demandeur " +
            "WHERE p.valideurId IS NULL AND p.statut = 'EN_ATTENTE' ORDER BY p.dateCreation ASC")
    List<DemandePermission> findAllOrphelines();
}
