// ─── CandidatureRepository.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    // ─── Multi-tenant ─────────────────────────────────────────
    List<Candidature> findByEntrepriseId(Long entrepriseId);

    // Fetch-joins (Hibernate 6 : évite l'erreur "Illegal pop()" sur les proxies lazy)
    @Query("SELECT DISTINCT c FROM Candidature c LEFT JOIN FETCH c.offre WHERE c.entreprise.id = :entrepriseId")
    List<Candidature> findByEntrepriseIdWithOffre(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT c FROM Candidature c LEFT JOIN FETCH c.offre")
    List<Candidature> findAllWithOffre();

    @Query("SELECT DISTINCT c FROM Candidature c LEFT JOIN FETCH c.offre WHERE c.offre.id = :offreId")
    List<Candidature> findByOffreIdWithOffre(@Param("offreId") Long offreId);

    // ─── Refonte 5.7 : scoping départemental (LEAD = ses départements) ──────
    /** Candidatures de l'entreprise dont l'offre appartient aux départements donnés (fetch offre). */
    @Query("SELECT DISTINCT c FROM Candidature c LEFT JOIN FETCH c.offre o LEFT JOIN FETCH o.departement " +
            "WHERE c.entreprise.id = :entrepriseId AND o.departement.id IN :departementIds")
    List<Candidature> findByEntrepriseIdAndOffreDepartementIn(@Param("entrepriseId") Long entrepriseId,
                                                              @Param("departementIds") List<Long> departementIds);

    /** Nombre de candidatures d'une offre (évite de charger la collection — fix N+1). */
    long countByOffreId(Long offreId);

    @Query("SELECT c FROM Candidature c LEFT JOIN FETCH c.offre WHERE c.id = :id")
    Candidature findByIdWithOffre(@Param("id") Long id);

    // ─── Filtres ──────────────────────────────────────────────
    List<Candidature> findByOffreId(Long offreId);
    List<Candidature> findByEntrepriseIdAndStatut(Long entrepriseId, Candidature.StatutCandidature statut);
    List<Candidature> findByStatut(Candidature.StatutCandidature statut);
    boolean existsByEmailIgnoreCaseAndOffreId(String email, Long offreId);
}