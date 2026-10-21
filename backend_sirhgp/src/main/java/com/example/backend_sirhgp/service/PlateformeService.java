package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.PlateformeConfigRequestDTO;
import com.example.backend_sirhgp.dto.response.BackupResponseDTO;
import com.example.backend_sirhgp.dto.response.PlateformeConfigResponseDTO;
import com.example.backend_sirhgp.dto.response.UtilisateurCibleDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.BackupLog;
import com.example.backend_sirhgp.model.PlateformeConfig;
import com.example.backend_sirhgp.model.VersionRefonte;
import com.example.backend_sirhgp.repository.BackupLogRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.PlateformeConfigRepository;
import com.example.backend_sirhgp.repository.VersionRefonteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refonte 5.9 (§5) — administration technique de la plateforme :
 * santé, configuration globale, versions 5.x, sauvegardes, utilisateurs d'un tenant
 * (liste de support pour l'impersonation — le SUPER_ADMIN n'a pas accès /api/employers).
 * Les feature flags ont leur propre service (FeatureFlagService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private final PlateformeConfigRepository configRepository;
    private final VersionRefonteRepository versionRepository;
    private final BackupLogRepository backupLogRepository;
    private final EmployerRepository employerRepository;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    // ─── §5 — Santé ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            out.put("db", "OK");
        } catch (Exception e) {
            out.put("db", "KO");
        }
        try {
            Path path = Paths.get(".").toAbsolutePath().normalize();
            FileStore store = Files.getFileStore(path);
            out.put("disqueLibreMo", store.getUsableSpace() / (1024 * 1024));
        } catch (Exception e) {
            out.put("disqueLibreMo", null);
        }
        out.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        out.put("timestamp", java.time.LocalDateTime.now().toString());
        return out;
    }

    // ─── §5 — Configuration globale ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PlateformeConfigResponseDTO> findAllConfig() {
        return configRepository.findAll().stream().map(this::toConfigDTO).toList();
    }

    @Transactional
    public PlateformeConfigResponseDTO upsertConfig(PlateformeConfigRequestDTO dto) {
        String cle = dto.getCle().trim();
        PlateformeConfig cfg = configRepository.findByCle(cle).orElseGet(PlateformeConfig::new);
        cfg.setCle(cle);
        cfg.setValeur(dto.getValeur());
        if (dto.getDescription() != null) cfg.setDescription(dto.getDescription());
        cfg = configRepository.save(cfg);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Modification de la config plateforme '" + cle + "'", "PlateformeConfig", cfg.getId(), cle);
        return toConfigDTO(cfg);
    }

    // ─── §5 — Versions / refontes ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findVersions() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (VersionRefonte v : versionRepository.findAllByOrderByNumeroDesc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("numero", v.getNumero());
            m.put("dateSortie", v.getDateSortie());
            m.put("resume", v.getResume());
            out.add(m);
        }
        return out;
    }

    /** Seed de l'historique des refontes (une fois, si la table est vide). */
    @Transactional
    public void seedVersions() {
        if (versionRepository.count() > 0) return;
        versionRepository.saveAll(List.of(
                VersionRefonte.builder().numero("2.0").resume("Landing publique + pages d'authentification séparées").build(),
                VersionRefonte.builder().numero("3.4").resume("Sucursales multi-tenant + scoping fin par sucursal").build(),
                VersionRefonte.builder().numero("4.2").resume("Configuration du pointage par entreprise").build(),
                VersionRefonte.builder().numero("5.0").resume("Refonte logique des rôles : SUPER_ADMIN = opérateur plateforme").build(),
                VersionRefonte.builder().numero("5.5").resume("Chefs de département (désignation superviseur, LEAD/ADMIN)").build(),
                VersionRefonte.builder().numero("5.6").resume("Demandes de permission (workflow STAFF→LEAD→MANAGER→ADMIN)").build(),
                VersionRefonte.builder().numero("5.7").resume("Recrutement : scope départemental du LEAD").build(),
                VersionRefonte.builder().numero("5.8").resume("ADMIN hérite des droits du LEAD (gestion_equipe_limitee)").build(),
                VersionRefonte.builder().numero("5.9").dateSortie(LocalDate.now())
                        .resume("SUPER_ADMIN : cadrage plateforme, RBAC, impersonation, quotas, feature flags").build()
        ));
        log.info("→ Historique des refontes seedé");
    }

    // ─── §5 — Sauvegardes ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BackupResponseDTO> findBackups() {
        return backupLogRepository.findTop50ByOrderByIdDesc().stream().map(this::toBackupDTO).toList();
    }

    @Transactional
    public BackupResponseDTO requestBackup() {
        BackupLog b = backupLogRepository.save(BackupLog.builder()
                .statut(BackupLog.StatutBackup.DEMANDE)
                .message("Sauvegarde demandée — exécution par le script infrastructure")
                .build());
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Demande de sauvegarde plateforme (id=" + b.getId() + ")", "BackupLog", b.getId(), null);
        return toBackupDTO(b);
    }

    @Transactional
    public BackupResponseDTO updateBackupStatut(Long id, BackupLog.StatutBackup statut, String message) {
        BackupLog b = backupLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sauvegarde introuvable : " + id));
        b.setStatut(statut);
        if (message != null) b.setMessage(message);
        b = backupLogRepository.save(b);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Sauvegarde " + b.getId() + " → " + statut, "BackupLog", b.getId(), null);
        return toBackupDTO(b);
    }

    // ─── §6 — Utilisateurs d'un tenant (cibles d'impersonation) ────────────────

    @Transactional(readOnly = true)
    public List<UtilisateurCibleDTO> utilisateurs(Long entrepriseId) {
        return employerRepository.findByEntrepriseId(entrepriseId).stream()
                .map(e -> new UtilisateurCibleDTO(
                        e.getId(), e.getName(), e.getEmail(),
                        e.getRole() != null ? e.getRole().getName() : null,
                        e.isActif()))
                .toList();
    }

    // ─── Utilitaires ───────────────────────────────────────────────────────────

    private PlateformeConfigResponseDTO toConfigDTO(PlateformeConfig c) {
        return new PlateformeConfigResponseDTO(c.getId(), c.getCle(), c.getValeur(),
                c.getDescription(), c.getDateModification());
    }

    private BackupResponseDTO toBackupDTO(BackupLog b) {
        return new BackupResponseDTO(b.getId(), b.getDateCreation(), b.getStatut().name(), b.getMessage());
    }
}
