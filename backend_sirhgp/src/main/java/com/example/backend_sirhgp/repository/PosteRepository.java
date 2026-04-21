package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosteRepository extends JpaRepository<Poste, Long> {

    boolean existsByName(String name);
    Optional<Poste> findByName(String name);
    long countByName(String name);   // utile pour certains contrôles

    @Query("SELECT p FROM Poste p LEFT JOIN FETCH p.permissions WHERE p.id = :id")
    Optional<Poste> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Poste p LEFT JOIN FETCH p.permissions")
    List<Poste> findAllWithPermissions();

    @Query("SELECT p FROM Poste p " +
            "LEFT JOIN FETCH p.departement " +
            "LEFT JOIN FETCH p.role " +
            "WHERE p.id = :id")
    Optional<Poste> findByIdWithDetails(@Param("id") Long id);

    List<Poste> findByRoleId(Long roleId);
    List<Poste> findByDepartementId(Long departementId);

    @Query("SELECT COUNT(e) FROM Employer e WHERE e.poste.id = :posteId AND e.actif = true")
    long countEmployesActifsByPosteId(@Param("posteId") Long posteId);
}