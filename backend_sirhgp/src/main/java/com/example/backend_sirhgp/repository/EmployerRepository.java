package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    Optional<Employer> findByEmail(String email);

    /**
     * CORRECTION : ajout de LEFT JOIN FETCH p.departement
     * pour éviter LazyInitializationException sur Departement
     * lors de la construction du UserSummaryDTO dans AuthService.
     */
    @Query("SELECT DISTINCT e FROM Employer e " +
            "LEFT JOIN FETCH e.role r " +
            "LEFT JOIN FETCH r.permissions rp " +
            "LEFT JOIN FETCH e.poste p " +
            "LEFT JOIN FETCH p.role pr " +
            "LEFT JOIN FETCH p.departement " +  // ← LIGNE AJOUTÉE
            "LEFT JOIN FETCH p.permissions pp " +
            "LEFT JOIN FETCH e.entreprise " +
            "LEFT JOIN FETCH e.sucursal " +
            "LEFT JOIN FETCH e.manager " +
            "WHERE e.email = :email")
    Optional<Employer> findByEmailWithFullContext(@Param("email") String email);

    @Query("SELECT e FROM Employer e " +
            "LEFT JOIN FETCH e.role " +
            "LEFT JOIN FETCH e.poste p " +
            "LEFT JOIN FETCH p.role " +
            "LEFT JOIN FETCH p.departement " +
            "LEFT JOIN FETCH e.entreprise " +
            "LEFT JOIN FETCH e.sucursal " +
            "LEFT JOIN FETCH e.manager " +
            "WHERE e.email = :email")
    Optional<Employer> findByEmailWithRolesAndPoste(@Param("email") String email);

    @Query("SELECT e FROM Employer e " +
            "LEFT JOIN FETCH e.role " +
            "LEFT JOIN FETCH e.poste p " +
            "LEFT JOIN FETCH p.departement " +
            "LEFT JOIN FETCH e.entreprise " +
            "LEFT JOIN FETCH e.sucursal " +
            "LEFT JOIN FETCH e.manager " +
            "WHERE e.id = :id")
    Optional<Employer> findByIdWithDetails(@Param("id") Long id);

    List<Employer> findByActifTrue();
    List<Employer> findByPosteId(Long posteId);
    List<Employer> findByRoleId(Long roleId);
    List<Employer> findByEntrepriseId(Long entrepriseId);
    List<Employer> findBySucursalIdAndActifTrueOrderByNameAsc(Long sucursalId);
    long countBySucursalId(Long sucursalId);

    @Query("SELECT COUNT(e) FROM Employer e WHERE e.sucursal.id = :sucursalId AND e.actif = true AND UPPER(e.role.name) = 'SECRETAIRE' AND (:employerId IS NULL OR e.id <> :employerId)")
    long countOtherActiveSecretaries(@Param("sucursalId") Long sucursalId, @Param("employerId") Long employerId);

    @Query("SELECT e.entreprise.id, COUNT(e) FROM Employer e WHERE e.entreprise IS NOT NULL GROUP BY e.entreprise.id")
    List<Object[]> countGroupedByEntreprise();

    @Query("SELECT e FROM Employer e WHERE e.actif = true AND UPPER(e.role.name) = 'MANAGER' AND e.entreprise.id = :entrepriseId")
    List<Employer> findManagersActifsByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT e FROM Employer e WHERE e.actif = true AND UPPER(e.role.name) = 'MANAGER'")
    List<Employer> findAllManagersActifs();

    @Query("SELECT e FROM Employer e WHERE UPPER(e.role.name) = 'ADMIN' AND e.entreprise.id = :entrepriseId")
    List<Employer> findAdminsByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT e.poste.departement.id FROM Employer e WHERE e.id = :employerId")
    Optional<Long> findDepartementIdOfPoste(@Param("employerId") Long employerId);

    @Query("SELECT DISTINCT e.poste.departement.id FROM Employer e " +
            "WHERE e.entreprise.id = :entrepriseId AND e.sucursal.id = :sucursalId " +
            "AND e.poste.departement.id IS NOT NULL")
    List<Long> findDepartementIdsByEntrepriseAndSucursal(@Param("entrepriseId") Long entrepriseId,
                                                          @Param("sucursalId") Long sucursalId);

    @Query("SELECT e.id FROM Employer e WHERE e.poste.departement.id IN :departementIds")
    List<Long> findIdsByDepartementIdIn(@Param("departementIds") List<Long> departementIds);

    long countByPosteId(Long posteId);
    long countByRoleId(Long roleId);

    long countByEntrepriseId(Long entrepriseId);

    @Query("SELECT COUNT(e) FROM Employer e WHERE e.entreprise.id = :entrepriseId AND e.actif = true AND UPPER(e.role.name) = 'ADMIN'")
    long countActiveAdminsByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT COUNT(e) FROM Employer e " +
            "WHERE e.poste.departement.id = :departementId AND e.actif = true")
    long countActifsByDepartementId(@Param("departementId") Long departementId);

    @Query("SELECT COUNT(e) FROM Employer e " +
            "WHERE e.poste.departement.id = :departementId AND e.entreprise.id = :entrepriseId AND e.actif = true")
    long countActifsByDepartementIdAndEntrepriseId(@Param("departementId") Long departementId,
                                                   @Param("entrepriseId") Long entrepriseId);
}
