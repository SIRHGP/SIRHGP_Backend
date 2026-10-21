package com.example.backend_sirhgp.repository;

import com.example.backend_sirhgp.model.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Refonte 5.9 (§5) — journal des sauvegardes. */
@Repository
public interface BackupLogRepository extends JpaRepository<BackupLog, Long> {
    List<BackupLog> findTop50ByOrderByIdDesc();
}