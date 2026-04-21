package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.model.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CORRECTIONS :
 * - findByChefProjet(Employer) et findByMembresContaining(Employer) remplacés par ID.
 * - findByIdWithDetails() découpé en plusieurs queries ciblées pour éviter le produit
 *   cartésien : charger membres + departements + taches en un seul JOIN FETCH produit
 *   N*M*P lignes de résultat → timeout sur les gros projets.
 * - sumBudgetAlloue() et sumBudgetConsomme() : Double → BigDecimal.
 * - findProjetsEnRetard() corrigé : requête séparée par statut.
 * - COALESCE ajouté sur les SUM pour éviter les null.
 */
@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    // ─── Filtres simples ──────────────────────────────────────

    List<Projet> findByChefProjetId(Long chefProjetId);
    List<Projet> findByStatut(Projet.StatutProjet statut);
    Optional<Projet> findByCodeProjet(String codeProjet);
    boolean existsByCodeProjet(String codeProjet);
    Optional<Projet> findByNom(String nom);
    boolean existsByNom(String nom);

    List<Projet> findByDateFinPrevueBeforeAndStatutNot(LocalDate date, Projet.StatutProjet statut);

    /**
     * Projets d'un membre spécifique.
     */
    @Query("SELECT p FROM Projet p JOIN p.membres m WHERE m.id = :membreId")
    List<Projet> findByMembreId(@Param("membreId") Long membreId);

    @Query("SELECT p FROM Projet p JOIN p.departements d WHERE d.id = :deptId")
    List<Projet> findByDepartementId(@Param("deptId") Long deptId);

    // ─── Chargement avec relations (SÉPARÉS pour éviter produit cartésien) ────

    /**
     * Charge le projet avec son chef et ses membres.
     * CORRECTION : on ne charge PAS taches et departements en même temps
     * pour éviter le produit cartésien SQL.
     */
    @Query("SELECT DISTINCT p FROM Projet p " +
            "LEFT JOIN FETCH p.chefProjet " +
            "LEFT JOIN FETCH p.membres " +
            "WHERE p.id = :id")
    Optional<Projet> findByIdWithMembres(@Param("id") Long id);

    /**
     * Charge le projet avec ses départements.
     */
    @Query("SELECT DISTINCT p FROM Projet p " +
            "LEFT JOIN FETCH p.departements " +
            "WHERE p.id = :id")
    Optional<Projet> findByIdWithDepartements(@Param("id") Long id);

    /**
     * Charge le projet avec ses factures.
     */
    @Query("SELECT DISTINCT p FROM Projet p " +
            "LEFT JOIN FETCH p.factures " +
            "WHERE p.id = :id")
    Optional<Projet> findByIdWithFactures(@Param("id") Long id);

    // ─── Projets en retard ────────────────────────────────────

    @Query("SELECT p FROM Projet p " +
            "WHERE p.statut = :statut " +
            "AND p.dateFinPrevue < :date " +
            "AND p.progression < 100")
    List<Projet> findProjetsEnRetard(
            @Param("statut") Projet.StatutProjet statut,
            @Param("date") LocalDate date
    );

    // ─── Compteurs pour le dashboard (évite N+1) ──────────────

    @Query("SELECT COUNT(p) FROM Projet p WHERE p.statut = :statut")
    long countByStatut(@Param("statut") Projet.StatutProjet statut);

    @Query("SELECT COUNT(t) FROM Projet p JOIN p.taches t WHERE t.statut = :statut")
    long countTachesByStatut(@Param("statut") Tache.StatutTache statut);

    @Query("SELECT COUNT(t) FROM Projet p JOIN p.taches t")
    long countTotalTaches();

    @Query("SELECT COUNT(t) FROM Projet p JOIN p.taches t WHERE p.id = :projetId")
    long countTachesByProjetId(@Param("projetId") Long projetId);

    @Query("SELECT COUNT(t) FROM Projet p JOIN p.taches t WHERE p.id = :projetId AND t.statut = :statut")
    long countTachesByProjetIdAndStatut(
            @Param("projetId") Long projetId,
            @Param("statut") Tache.StatutTache statut
    );

    // ─── Agrégats budgétaires (BigDecimal) ────────────────────

    /**
     * CORRECTION : Double → BigDecimal + COALESCE pour éviter null si aucun projet.
     */
    @Query("SELECT COALESCE(SUM(p.budgetAlloue), 0) FROM Projet p")
    BigDecimal sumBudgetAlloue();

    @Query("SELECT COALESCE(SUM(p.budgetConsomme), 0) FROM Projet p")
    BigDecimal sumBudgetConsomme();

    @Query("SELECT COALESCE(SUM(p.budgetAlloue), 0) FROM Projet p WHERE p.statut = :statut")
    BigDecimal sumBudgetAlloueByStatut(@Param("statut") Projet.StatutProjet statut);

    // ─── Stats dashboard ──────────────────────────────────────

    /**
     * Progression moyenne de tous les projets actifs.
     */
    @Query("SELECT COALESCE(AVG(p.progression), 0) FROM Projet p " +
            "WHERE p.statut NOT IN (:statutTermine, :statutAnnule)")
    Double avgProgressionProjetsActifs(
            @Param("statutTermine") Projet.StatutProjet statutTermine,
            @Param("statutAnnule") Projet.StatutProjet statutAnnule
    );
}