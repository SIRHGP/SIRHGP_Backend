package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByName(String name);

    Optional<Permission> findByName(String name);

    @Query("SELECT p FROM Permission p LEFT JOIN FETCH p.actions WHERE p.id = :id")
    Optional<Permission> findByIdWithActions(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Permission p LEFT JOIN FETCH p.actions")
    List<Permission> findAllWithActions();

    @Query("SELECT p FROM Permission p LEFT JOIN FETCH p.actions WHERE p.name = :name")
    Optional<Permission> findByNameWithActions(@Param("name") String name);

    @Query(value = "SELECT DISTINCT p FROM Permission p LEFT JOIN FETCH p.actions ORDER BY p.id",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Permission p")
    Page<Permission> findAllWithActions(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r) FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    long countRolesByPermissionId(@Param("permissionId") Long permissionId);

    @Query("SELECT COUNT(DISTINCT po) FROM Poste po JOIN po.permissions perm WHERE perm.id = :permissionId")
    long countPostesByPermissionId(@Param("permissionId") Long permissionId);
}