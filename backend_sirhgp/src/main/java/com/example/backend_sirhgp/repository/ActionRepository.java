package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour Action.
 * Pas de modification — déjà correct dans l'original.
 */
@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    boolean existsByName(String name);

    Optional<Action> findByName(String name);
}