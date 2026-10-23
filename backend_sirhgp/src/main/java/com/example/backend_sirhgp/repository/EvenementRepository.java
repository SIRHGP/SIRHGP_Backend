package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CORRECTIONS :
 * - findByCreateur(Employer) et findByParticipantsContaining(Employer) remplacés
 *   par des queries par ID.
 * - Ajout de findByProjetId() — manquait alors que Evenement a une relation projet.
 * - Ajout de findByIdWithRelations() pour éviter N+1.
 * - Ajout de countByProjetId() et countByCreateurId() pour les stats.
 * - findByRappelActif avec LocalDate gardé (cohérent avec le modèle).
 */
@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    List<Evenement> findBySucursalId(Long sucursalId);
    List<Evenement> findByEntrepriseId(Long entrepriseId);
    List<Evenement> findBySucursalIdAndDateDebutBetween(Long sucursalId, LocalDate start, LocalDate end);
    List<Evenement> findByEntrepriseIdAndDateDebutBetween(Long entrepriseId, LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT e FROM Evenement e JOIN e.participants p WHERE p.id = :participantId AND e.sucursal.id = :sucursalId")
    List<Evenement> findByParticipantIdAndSucursalId(@Param("participantId") Long participantId, @Param("sucursalId") Long sucursalId);

    List<Evenement> findByProjetIdAndSucursalId(Long projetId, Long sucursalId);

    // ─── Filtres simples ──────────────────────────────────────

    List<Evenement> findByCreateurId(Long createurId);

    List<Evenement> findByProjetId(Long projetId);  // CORRECTION : manquait

    List<Evenement> findByType(Evenement.TypeEvenement type);

    // ─── Filtres par participant ───────────────────────────────

    /**
     * CORRECTION : requête JPQL au lieu de findByParticipantsContaining(Employer).
     * findByParticipantsContaining charge l'entité Employer complète inutilement.
     */
    @Query("SELECT e FROM Evenement e JOIN e.participants p WHERE p.id = :participantId")
    List<Evenement> findByParticipantId(@Param("participantId") Long participantId);

    // ─── Filtres par date ─────────────────────────────────────

    List<Evenement> findByDateDebutBetween(LocalDate start, LocalDate end);

    List<Evenement> findByDateDebut(LocalDate date);

    /**
     * Événements futurs à partir d'une date donnée.
     */
    @Query("SELECT e FROM Evenement e WHERE e.dateDebut >= :date ORDER BY e.dateDebut ASC")
    List<Evenement> findEvenementsAVenir(@Param("date") LocalDate date);

    /**
     * Événements passés avant une date donnée.
     */
    @Query("SELECT e FROM Evenement e WHERE e.dateFin < :date ORDER BY e.dateFin DESC")
    List<Evenement> findEvenementsPasses(@Param("date") LocalDate date);

    // ─── Rappels ─────────────────────────────────────────────

    List<Evenement> findByRappelActifTrueAndDateDebutBetween(LocalDate start, LocalDate end);

    // ─── Chargement avec relations ────────────────────────────

    /**
     * Charge un événement avec createur, participants et projet en un seul SELECT.
     */
    @Query("SELECT e FROM Evenement e " +
            "LEFT JOIN FETCH e.createur " +
            "LEFT JOIN FETCH e.participants " +
            "LEFT JOIN FETCH e.projet " +
            "WHERE e.id = :id")
    Optional<Evenement> findByIdWithRelations(@Param("id") Long id);

    // ─── Compteurs pour les ResponseDTOs ──────────────────────

    long countByCreateurId(Long createurId);
    long countByProjetId(Long projetId);

    @Query("SELECT COUNT(e) FROM Evenement e JOIN e.participants p WHERE p.id = :participantId")
    long countByParticipantId(@Param("participantId") Long participantId);
}
