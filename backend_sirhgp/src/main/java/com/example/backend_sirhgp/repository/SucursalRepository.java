package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByEntrepriseIdOrderByNameAsc(Long entrepriseId);

    boolean existsByEntrepriseIdAndName(Long entrepriseId, String name);

    /**
     * Sucursales dont l'employé est le manager (règle d'unicité : un employé ne peut
     * être manager que d'UNE SEULE sucursale). Utilisé par SucursalService.assignManager
     * pour refuser une double affectation avec un message explicite.
     */
    List<Sucursal> findByManagerId(Long managerId);

    /** Sucursales de l'entreprise de l'utilisateur courant (scoping front). */
    @Query("SELECT s FROM Sucursal s WHERE s.entreprise.id = :entrepriseId AND s.statut = 'ACTIVE' ORDER BY s.name")
    List<Sucursal> findActiveByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
}