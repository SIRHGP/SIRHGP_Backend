package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CORRECTIONS :
 * - findByEmployer(Employer) et findByUploader(Employer) remplacés par leurs
 *   équivalents par ID — pas besoin de charger l'entité entière pour filtrer.
 * - Ajout de findByProjetId() — manquait alors que Document a une relation projet.
 * - Ajout de countByEmployerId() et countByProjetId() pour les stats des ResponseDTOs.
 * - Ajout de findByIdWithRelations() pour éviter N+1 lors du chargement d'un document.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByNomFichier(String nomFichier);
    boolean existsByNomFichier(String nomFichier);

    // ─── Filtres par ID (plus efficaces que par entité) ───────

    List<Document> findByEmployerId(Long employerId);
    List<Document> findByUploaderId(Long uploaderId);

    // CORRECTION : manquait dans l'original
    List<Document> findByProjetId(Long projetId);

    List<Document> findByType(Document.TypeDocument type);

    List<Document> findByPubliqueTrue();

    // ─── Compteurs pour les ResponseDTOs (évite N+1) ──────────

    long countByEmployerId(Long employerId);
    long countByUploaderId(Long uploaderId);
    long countByProjetId(Long projetId);

    // ─── Chargement avec relations ────────────────────────────

    /**
     * Charge un document avec employer, uploader et projet en un seul SELECT.
     * À utiliser dans DocumentService.getById() pour éviter les lazy loads en cascade.
     */
    @Query("SELECT d FROM Document d " +
            "LEFT JOIN FETCH d.employer " +
            "LEFT JOIN FETCH d.uploader " +
            "LEFT JOIN FETCH d.projet " +
            "WHERE d.id = :id")
    Optional<Document> findByIdWithRelations(@Param("id") Long id);

    /**
     * Liste les documents publics d'un projet (pour affichage sans authentification).
     */
    @Query("SELECT d FROM Document d " +
            "WHERE d.projet.id = :projetId AND d.publique = true")
    List<Document> findPublicsByProjetId(@Param("projetId") Long projetId);
}