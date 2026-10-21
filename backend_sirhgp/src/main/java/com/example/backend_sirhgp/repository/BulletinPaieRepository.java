// ─── BulletinPaieRepository.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.BulletinPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepository extends JpaRepository<BulletinPaie, Long> {

    Optional<BulletinPaie> findByEmployeIdAndMoisAndAnnee(Long employeId, Integer mois, Integer annee);

    List<BulletinPaie> findByEmployeIdOrderByAnneeDescMoisDesc(Long employeId);

    // Scoping par entreprise
    org.springframework.data.domain.Page<BulletinPaie> findByEntrepriseId(Long entrepriseId, org.springframework.data.domain.Pageable pageable);
    List<BulletinPaie> findByEntrepriseId(Long entrepriseId);

    @Query("SELECT b FROM BulletinPaie b WHERE b.entreprise.id = :entrepriseId " +
            "AND b.mois = :mois AND b.annee = :annee")
    List<BulletinPaie> findByEntrepriseIdAndPeriode(@Param("entrepriseId") Long entrepriseId,
                                                    @Param("mois") Integer mois,
                                                    @Param("annee") Integer annee);

    // ─── Scoping succursale (MANAGER / LEAD ancré) : via la sucursale de l'employé ───
    @Query("SELECT b FROM BulletinPaie b WHERE b.entreprise.id = :entrepriseId " +
            "AND b.employe.sucursal.id = :sucursalId")
    List<BulletinPaie> findByEntrepriseIdAndSucursalId(@Param("entrepriseId") Long entrepriseId,
                                                       @Param("sucursalId") Long sucursalId);

    @Query("SELECT b FROM BulletinPaie b WHERE b.entreprise.id = :entrepriseId " +
            "AND b.employe.sucursal.id = :sucursalId AND b.mois = :mois AND b.annee = :annee")
    List<BulletinPaie> findByEntrepriseIdAndSucursalIdAndPeriode(@Param("entrepriseId") Long entrepriseId,
                                                                 @Param("sucursalId") Long sucursalId,
                                                                 @Param("mois") Integer mois,
                                                                 @Param("annee") Integer annee);

    // Charge le bulletin avec son employé + poste + entreprise (génération PDF hors session)
    @Query("SELECT b FROM BulletinPaie b LEFT JOIN FETCH b.employe e LEFT JOIN FETCH e.poste " +
            "LEFT JOIN FETCH b.entreprise WHERE b.id = :id")
    Optional<BulletinPaie> findByIdWithEmploye(@Param("id") Long id);
}