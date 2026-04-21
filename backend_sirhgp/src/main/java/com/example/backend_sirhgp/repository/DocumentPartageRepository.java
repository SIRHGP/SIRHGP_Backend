package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.DocumentPartage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentPartageRepository extends JpaRepository<DocumentPartage, Long> {

    // Tous les partages reçus par un employé
    List<DocumentPartage> findByDestinataireIdOrderByDateEnvoiDesc(Long destinataireId);

    // Tous les partages envoyés par un employé
    List<DocumentPartage> findByExpediteurIdOrderByDateEnvoiDesc(Long expediteurId);

    // Tous les partages d'un document précis
    List<DocumentPartage> findByDocumentId(Long documentId);

    // Vérifie si un employé a accès à un document (expéditeur OU destinataire)
    @Query("""
        SELECT COUNT(p) > 0 FROM DocumentPartage p
        WHERE p.document.id = :documentId
        AND (p.expediteur.id = :employerId OR p.destinataire.id = :employerId)
    """)
    boolean hasAccess(@Param("documentId") Long documentId,
                      @Param("employerId") Long employerId);

    // Nombre de partages non lus pour un destinataire
    long countByDestinataireIdAndLuFalse(Long destinataireId);

    // Marquer tous les partages d'un doc comme lus pour un destinataire
    @Query("""
        UPDATE DocumentPartage p SET p.lu = true
        WHERE p.destinataire.id = :destinataireId
        AND p.document.id = :documentId
    """)
    @org.springframework.data.jpa.repository.Modifying
    void marquerLus(@Param("destinataireId") Long destinataireId,
                    @Param("documentId") Long documentId);
}