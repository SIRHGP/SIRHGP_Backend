package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ─── Documents reçus par un employé ──────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE d.destinataire.id = :employerId
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    List<Document> findReceivedDocuments(@Param("employerId") Long employerId,
                                         @Param("status") DocumentStatus status);

    @Query("""
        SELECT d FROM Document d
        WHERE d.destinataire.id = :employerId
        AND d.estLu = false
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    List<Document> findUnreadReceivedDocuments(@Param("employerId") Long employerId,
                                               @Param("status") DocumentStatus status);

    // ─── Documents envoyés par un employé ────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE d.expediteur.id = :employerId
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    List<Document> findSentDocuments(@Param("employerId") Long employerId,
                                     @Param("status") DocumentStatus status);

    // ─── Documents d'un utilisateur (expéditeur OU destinataire) ─────────────
    @Query("""
        SELECT d FROM Document d
        WHERE (d.expediteur.id = :employerId OR d.destinataire.id = :employerId)
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    Page<Document> findAllByUser(@Param("employerId") Long employerId,
                                 @Param("status") DocumentStatus status,
                                 Pageable pageable);

    // ─── Documents publics ────────────────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE d.publique = true
        AND d.status = :status
        AND d.destinataire IS NULL
    """)
    List<Document> findPublicDocuments(@Param("status") DocumentStatus status);

    // ─── Tous les documents par statut (admin) ────────────────────────────────
    @Query("SELECT d FROM Document d WHERE d.status = :status")
    Page<Document> findAllByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    // ─── Chargement avec relations ────────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        LEFT JOIN FETCH d.employer
        LEFT JOIN FETCH d.expediteur
        LEFT JOIN FETCH d.destinataire
        LEFT JOIN FETCH d.projet
        WHERE d.id = :id
    """)
    Optional<Document> findByIdWithRelations(@Param("id") Long id);

    // ─── Vérification de références actives sur un fichier physique ───────────
    // Remplace le findAll().stream() coûteux dans deleteDocument()
    @Query("""
        SELECT COUNT(d) FROM Document d
        WHERE d.cheminStockage = :chemin
        AND d.status <> :statusExclu
        AND d.id <> :excludeId
    """)
    long countActiveByCheminStockage(@Param("chemin") String chemin,
                                     @Param("statusExclu") DocumentStatus statusExclu,
                                     @Param("excludeId") Long excludeId);

    // ─── Filtres simples ──────────────────────────────────────────────────────
    List<Document> findByEmployerId(Long employerId);
    List<Document> findByProjetId(Long projetId);

    // ─── Compteurs ───────────────────────────────────────────────────────────
    long countByEmployerId(Long employerId);
    long countByExpediteurId(Long expediteurId);
    long countByProjetId(Long projetId);
    long countByDestinataireIdAndEstLuFalseAndStatus(Long destinataireId, DocumentStatus status);
    long countByExpediteurIdAndStatus(Long expediteurId, DocumentStatus status);

    // ─── Marquer comme lu ────────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE Document d SET d.estLu = true, d.dateLecture = CURRENT_TIMESTAMP
        WHERE d.id = :documentId AND d.destinataire.id = :employerId
    """)
    void markAsRead(@Param("documentId") Long documentId,
                    @Param("employerId") Long employerId);

    // ─── Archiver ─────────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Document d SET d.estArchive = true WHERE d.id = :id")
    void archiveDocument(@Param("id") Long id);

    // ─── Vérification d'accès ─────────────────────────────────────────────────
    @Query("""
        SELECT COUNT(d) > 0 FROM Document d
        WHERE d.id = :documentId
        AND (d.expediteur.id = :employerId OR d.destinataire.id = :employerId OR d.publique = true)
        AND d.status = :status
    """)
    boolean hasAccess(@Param("documentId") Long documentId,
                      @Param("employerId") Long employerId,
                      @Param("status") DocumentStatus status);

    long countByCheminStockageAndStatusNot(String chemin, DocumentStatus documentStatus);
}