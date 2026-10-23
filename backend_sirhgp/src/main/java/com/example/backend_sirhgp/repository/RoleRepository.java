package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByName(String name);

    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name <> 'SUPER_ADMIN' ORDER BY r.id")
    List<Role> findAllForFilters();

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions")
    List<Role> findAllWithPermissions();

    @Query(value = "SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions ORDER BY r.id",
            countQuery = "SELECT COUNT(DISTINCT r) FROM Role r")
    Page<Role> findAllWithPermissions(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employer e WHERE e.role.id = :roleId AND e.actif = true")
    long countEmployesActifsByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT COUNT(p) FROM Poste p WHERE p.role.id = :roleId")
    long countPostesByRoleId(@Param("roleId") Long roleId);
}
