package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CORRECTIONS :
 * - Ajout de findByIdWithPostes() avec JOIN FETCH pour éviter N+1
 *   lors du chargement des postes d'un département.
 * - Ajout de countEmployesByDepartementId() pour alimenter nombreEmployes
 *   dans le ResponseDTO sans charger toute la collection.
 * - Ajout de findByProjetId() pour lister les départements d'un projet.
 */
@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {

    boolean existsByNameAndEntrepriseId(String name, Long entrepriseId);

    List<Departement> findAllByName(String name);

    List<Departement> findBySuperviseurId(Long superviseurId);

    @Query("SELECT d.id FROM Departement d WHERE d.superviseur.id = :superviseurId")
    List<Long> findIdsBySuperviseurId(@Param("superviseurId") Long superviseurId);

    @Query("SELECT d.id FROM Departement d WHERE d.entreprise.id = :entrepriseId")
    List<Long> findIdsByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT d.id FROM Departement d WHERE d.entreprise.id = :entrepriseId AND " +
            "((d.superviseur.sucursal.id = :sucursalId AND d.superviseur.entreprise.id = :entrepriseId) " +
            "OR d.id IN (SELECT e.poste.departement.id FROM Employer e WHERE e.sucursal.id = :sucursalId " +
            "AND e.entreprise.id = :entrepriseId AND e.poste.departement IS NOT NULL))")
    List<Long> findIdsBySucursalId(@Param("sucursalId") Long sucursalId, @Param("entrepriseId") Long entrepriseId);

    /**
     * Charge un département avec ses postes en un seul SELECT.
     */
    @Query("SELECT d FROM Departement d LEFT JOIN FETCH d.postes WHERE d.id = :id")
    Optional<Departement> findByIdWithPostes(@Param("id") Long id);

    /**
     * Compte le nombre d'employés d'un département via la relation Poste → Employer.
     * Évite de charger tous les postes et tous les employés pour un simple COUNT.
     */
    @Query("SELECT COUNT(e) FROM Employer e " +
            "WHERE e.poste.departement.id = :departementId AND e.actif = true")
    long countEmployesActifsByDepartementId(@Param("departementId") Long departementId);

    /**
     * Liste les départements associés à un projet.
     * La relation est portée par Projet (owner), donc on requête depuis Projet.
     */
    @Query("SELECT d FROM Departement d JOIN d.postes p " +
            "WHERE EXISTS (" +
            "  SELECT proj FROM Projet proj JOIN proj.departements pd " +
            "  WHERE proj.id = :projetId AND pd.id = d.id" +
            ")")
    List<Departement> findByProjetId(@Param("projetId") Long projetId);

    /**
     * Version simplifiée et correcte via la relation inverse.
     */
    @Query("SELECT d FROM Departement d " +
            "JOIN Projet p ON d MEMBER OF p.departements " +
            "WHERE p.id = :projetId")
    List<Departement> findDepartementsByProjetId(@Param("projetId") Long projetId);
}
