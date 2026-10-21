// ─── OffreEmploiRepository.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {

    // ─── Multi-tenant ─────────────────────────────────────────
    List<OffreEmploi> findByEntrepriseId(Long entrepriseId);

    // Fetch-joins (Hibernate 6 : évite l'erreur "Illegal pop()" sur les proxies lazy)
    @Query("SELECT DISTINCT o FROM OffreEmploi o LEFT JOIN FETCH o.poste LEFT JOIN FETCH o.departement WHERE o.entreprise.id = :entrepriseId")
    List<OffreEmploi> findByEntrepriseIdWithRelations(@Param("entrepriseId") Long entrepriseId);

    // Refonte 5.7 : variante globale (aucun scope entreprise) pour ADMIN/SUPER_ADMIN
    @Query("SELECT DISTINCT o FROM OffreEmploi o LEFT JOIN FETCH o.poste LEFT JOIN FETCH o.departement")
    List<OffreEmploi> findAllWithRelations();

    // ─── Refonte 5.7 : scoping départemental (LEAD = ses départements) ──────
    /** Offres de l'entreprise rattachées aux départements donnés (fetch poste/département). */
    @Query("SELECT DISTINCT o FROM OffreEmploi o LEFT JOIN FETCH o.poste LEFT JOIN FETCH o.departement " +
            "WHERE o.entreprise.id = :entrepriseId AND o.departement.id IN :departementIds")
    List<OffreEmploi> findByEntrepriseIdAndDepartementIn(@Param("entrepriseId") Long entrepriseId,
                                                         @Param("departementIds") List<Long> departementIds);

    @Query("SELECT o FROM OffreEmploi o LEFT JOIN FETCH o.poste LEFT JOIN FETCH o.departement WHERE o.id = :id")
    OffreEmploi findByIdWithRelations(@Param("id") Long id);

    // ─── Filtres ──────────────────────────────────────────────
    List<OffreEmploi> findByEntrepriseIdAndStatut(Long entrepriseId, OffreEmploi.StatutOffre statut);
    List<OffreEmploi> findByStatut(OffreEmploi.StatutOffre statut);
    boolean existsByTitreIgnoreCaseAndEntrepriseId(String titre, Long entrepriseId);
}