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

    // ─── Documents reçus par un employé (destinataire) ───
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

    // ─── Documents envoyés par un employé (expéditeur) ───
    @Query("""
        SELECT d FROM Document d 
        WHERE d.expediteur.id = :employerId 
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    List<Document> findSentDocuments(@Param("employerId") Long employerId,
                                     @Param("status") DocumentStatus status);

    // ─── Documents d'un utilisateur (reçus + envoyés) ───
    @Query("""
        SELECT d FROM Document d 
        WHERE (d.expediteur.id = :employerId OR d.destinataire.id = :employerId)
        AND d.status = :status
        ORDER BY d.dateEnvoi DESC
    """)
    Page<Document> findAllByUser(@Param("employerId") Long employerId,
                                 @Param("status") DocumentStatus status,
                                 Pageable pageable);

    // ─── Documents publics ───
    @Query("""
        SELECT d FROM Document d 
        WHERE d.publique = true 
        AND d.status = :status
        AND d.destinataire IS NULL
    """)
    List<Document> findPublicDocuments(@Param("status") DocumentStatus status);

    // ─── Filtres par ID ───
    List<Document> findByEmployerId(Long employerId);
    List<Document> findByProjetId(Long projetId);

    // ─── COMPTEURS CORRIGÉS ───

    /**
     * Compte les documents associés à un employé (via la relation employer)
     */
    long countByEmployerId(Long employerId);

    /**
     * Compte les documents envoyés par un expéditeur (uploadés par cet employé)
     * Utilise expediteur au lieu de uploader
     */
    long countByExpediteurId(Long expediteurId);

    /**
     * Compte les documents associés à un projet
     */
    long countByProjetId(Long projetId);

    /**
     * Compte les documents reçus non lus par un destinataire
     */
    long countByDestinataireIdAndEstLuFalseAndStatus(Long destinataireId, DocumentStatus status);

    /**
     * Compte les documents envoyés par un expéditeur avec un statut spécifique
     */
    long countByExpediteurIdAndStatus(Long expediteurId, DocumentStatus status);

    // ─── Chargement avec relations ───
    @Query("""
        SELECT d FROM Document d 
        LEFT JOIN FETCH d.employer 
        LEFT JOIN FETCH d.expediteur 
        LEFT JOIN FETCH d.destinataire 
        LEFT JOIN FETCH d.projet 
        WHERE d.id = :id
    """)
    Optional<Document> findByIdWithRelations(@Param("id") Long id);

    // ─── Marquer comme lu ───
    @Modifying
    @Query("""
        UPDATE Document d SET d.estLu = true, d.dateLecture = CURRENT_TIMESTAMP
        WHERE d.id = :documentId AND d.destinataire.id = :employerId
    """)
    void markAsRead(@Param("documentId") Long documentId,
                    @Param("employerId") Long employerId);

    // ─── Archiver ───
    @Modifying
    @Query("UPDATE Document d SET d.estArchive = true WHERE d.id = :id")
    void archiveDocument(@Param("id") Long id);

    // ─── Vérification d'accès ───
    @Query("""
        SELECT COUNT(d) > 0 FROM Document d 
        WHERE d.id = :documentId 
        AND (d.expediteur.id = :employerId OR d.destinataire.id = :employerId OR d.publique = true)
        AND d.status = :status
    """)
    boolean hasAccess(@Param("documentId") Long documentId,
                      @Param("employerId") Long employerId,
                      @Param("status") DocumentStatus status);
}