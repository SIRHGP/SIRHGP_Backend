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
            "WHERE e.email = :email")
    Optional<Employer> findByEmailWithFullContext(@Param("email") String email);

    @Query("SELECT e FROM Employer e " +
            "LEFT JOIN FETCH e.role " +
            "LEFT JOIN FETCH e.poste p " +
            "LEFT JOIN FETCH p.role " +
            "LEFT JOIN FETCH p.departement " +
            "WHERE e.email = :email")
    Optional<Employer> findByEmailWithRolesAndPoste(@Param("email") String email);

    @Query("SELECT e FROM Employer e " +
            "LEFT JOIN FETCH e.role " +
            "LEFT JOIN FETCH e.poste p " +
            "LEFT JOIN FETCH p.departement " +
            "WHERE e.id = :id")
    Optional<Employer> findByIdWithDetails(@Param("id") Long id);

    List<Employer> findByActifTrue();
    List<Employer> findByPosteId(Long posteId);
    List<Employer> findByRoleId(Long roleId);

    long countByPosteId(Long posteId);
    long countByRoleId(Long roleId);

    @Query("SELECT COUNT(e) FROM Employer e " +
            "WHERE e.poste.departement.id = :departementId AND e.actif = true")
    long countActifsByDepartementId(@Param("departementId") Long departementId);
}