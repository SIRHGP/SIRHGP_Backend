package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CORRECTIONS :
 * - countByTacheId() SUPPRIMÉ — Tache n'a pas de relation vers elle-même,
 *   ce champ n'existe pas → erreur de compilation garantie dans l'original.
 * - findByAssignee(Employer), findByCreateur(Employer), findByProjet(Projet)
 *   remplacés par leurs équivalents par ID.
 * - Ajout de findByIdWithRelations() pour éviter N+1 sur assignee/createur/projet.
 * - Ajout de findByAssigneeIdAndStatut() pour les stats employé.
 * - Ajout de queries pour les tâches en retard avec JOIN FETCH.
 */
@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {

    // ─── Filtres simples ──────────────────────────────────────

    List<Tache> findByAssigneeId(Long assigneeId);
    List<Tache> findByCreateurId(Long createurId);
    List<Tache> findByProjetId(Long projetId);
    List<Tache> findByStatut(Tache.StatutTache statut);
    List<Tache> findByDateEcheance(LocalDate dateEcheance);

    List<Tache> findByDateEcheanceBeforeAndStatutNot(LocalDate date, Tache.StatutTache statut);

    List<Tache> findByAssigneeIdAndStatut(Long assigneeId, Tache.StatutTache statut);

    // ─── Compteurs pour les ResponseDTOs (évite N+1) ──────────

    long countByProjetId(Long projetId);
    long countByAssigneeId(Long assigneeId);

    long countByProjetIdAndStatut(Long projetId, Tache.StatutTache statut);
    long countByAssigneeIdAndStatut(Long assigneeId, Tache.StatutTache statut);

    /**
     * Compte les tâches en retard d'un projet.
     */
    @Query("SELECT COUNT(t) FROM Tache t " +
            "WHERE t.projet.id = :projetId " +
            "AND t.dateEcheance < :today " +
            "AND t.statut != :statutTerminee")
    long countTachesEnRetardByProjetId(
            @Param("projetId") Long projetId,
            @Param("today") LocalDate today,
            @Param("statutTerminee") Tache.StatutTache statutTerminee
    );

    /**
     * Compte les tâches en retard d'un assignee.
     */
    @Query("SELECT COUNT(t) FROM Tache t " +
            "WHERE t.assignee.id = :assigneeId " +
            "AND t.dateEcheance < :today " +
            "AND t.statut != :statutTerminee")
    long countTachesEnRetardByAssigneeId(
            @Param("assigneeId") Long assigneeId,
            @Param("today") LocalDate today,
            @Param("statutTerminee") Tache.StatutTache statutTerminee
    );

    // ─── Chargement avec relations (évite N+1) ────────────────

    /**
     * Charge une tâche avec assignee, createur et projet en un seul SELECT.
     * À utiliser dans TacheService.getById().
     */
    @Query("SELECT t FROM Tache t " +
            "LEFT JOIN FETCH t.assignee a " +
            "LEFT JOIN FETCH a.poste ap " +
            "LEFT JOIN FETCH ap.departement " +
            "LEFT JOIN FETCH t.createur c " +
            "LEFT JOIN FETCH c.poste cp " +
            "LEFT JOIN FETCH cp.departement " +
            "LEFT JOIN FETCH t.projet " +
            "WHERE t.id = :id")
    Optional<Tache> findByIdWithRelations(@Param("id") Long id);

    /**
     * Liste les tâches d'un projet avec leurs assignees (évite N+1 dans les listings).
     */
    @Query("SELECT t FROM Tache t " +
            "LEFT JOIN FETCH t.assignee " +
            "WHERE t.projet.id = :projetId " +
            "ORDER BY t.priorite DESC, t.dateEcheance ASC")
    List<Tache> findByProjetIdWithAssignee(@Param("projetId") Long projetId);
}