package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.CommentaireTache;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CORRECTIONS :
 * - findByTache(Tache) et deleteByTache(Tache) remplacés par leurs équivalents
 *   par ID — passer une entité complète en paramètre force Hibernate à la charger,
 *   alors qu'un findByTacheId() utilise directement la FK sans chargement inutile.
 * - findByTacheOrderByDateCreationDesc() remplacé par une query JPQL avec JOIN FETCH
 *   pour éviter N+1 sur auteur.
 * - Ajout de findRecentsByTacheId() avec Pageable pour charger les N derniers
 *   commentaires (utilisé dans TacheResponseDTO.commentairesRecents).
 */
@Repository
public interface CommentaireTacheRepository extends JpaRepository<CommentaireTache, Long> {

    /**
     * Charge les commentaires d'une tâche avec l'auteur en un seul SELECT.
     * CORRECTION : JOIN FETCH sur auteur pour éviter N+1.
     */
    @Query("SELECT c FROM CommentaireTache c " +
            "LEFT JOIN FETCH c.auteur " +
            "WHERE c.tache.id = :tacheId " +
            "ORDER BY c.dateCreation DESC")
    List<CommentaireTache> findByTacheIdWithAuteur(@Param("tacheId") Long tacheId);

    /**
     * Charge les N derniers commentaires pour l'affichage dans TacheResponseDTO.
     * Exemple d'appel : findRecentsByTacheId(tacheId, PageRequest.of(0, 3))
     */
    @Query("SELECT c FROM CommentaireTache c " +
            "LEFT JOIN FETCH c.auteur " +
            "WHERE c.tache.id = :tacheId " +
            "ORDER BY c.dateCreation DESC")
    List<CommentaireTache> findRecentsByTacheId(@Param("tacheId") Long tacheId, Pageable pageable);

    /**
     * COUNT pour éviter le N+1 dans le service lors du calcul de nombreCommentaires.
     */
    long countByTacheId(Long tacheId);

    /**
     * CORRECTION : deleteByTacheId au lieu de deleteByTache(Tache)
     * pour éviter le chargement inutile de l'entité Tache.
     */
    void deleteByTacheId(Long tacheId);
}