package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.PlateformeConfigRequestDTO;
import com.example.backend_sirhgp.dto.response.BackupResponseDTO;
import com.example.backend_sirhgp.dto.response.PlateformeConfigResponseDTO;
import com.example.backend_sirhgp.dto.response.QuotaResponseDTO;
import com.example.backend_sirhgp.dto.response.UtilisateurCibleDTO;
import com.example.backend_sirhgp.model.BackupLog;
import com.example.backend_sirhgp.model.Quota;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.PlateformeService;
import com.example.backend_sirhgp.service.QuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Refonte 5.9 (§5) — administration technique de la plateforme.
 * Toutes les routes sont réservées au SUPER_ADMIN (matcher /api/plateforme/** en doublon).
 */
@RestController
@RequestMapping("/api/plateforme")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlateformeController {

    private final PlateformeService plateformeService;
    private final QuotaService quotaService;

    // ─── §5 — Santé ────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Santé de la plateforme", plateformeService.health()));
    }

    // ─── §5 — Configuration globale ────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<PlateformeConfigResponseDTO>>> getConfig() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Configuration plateforme", plateformeService.findAllConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<PlateformeConfigResponseDTO>> putConfig(
            @Valid @RequestBody PlateformeConfigRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Configuration enregistrée", plateformeService.upsertConfig(dto)));
    }

    // ─── §5 — Versions / refontes ──────────────────────────────────────────────

    @GetMapping("/versions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> versions() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Historique des refontes", plateformeService.findVersions()));
    }

    // ─── §5 — Sauvegardes ──────────────────────────────────────────────────────

    @GetMapping("/backups")
    public ResponseEntity<ApiResponse<List<BackupResponseDTO>>> backups() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Sauvegardes", plateformeService.findBackups()));
    }

    @PostMapping("/backups")
    public ResponseEntity<ApiResponse<BackupResponseDTO>> requestBackup() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Sauvegarde demandée", plateformeService.requestBackup()));
    }

    /** Report du résultat par le script infrastructure (ou le SUPER_ADMIN). */
    @PatchMapping("/backups/{id}/statut")
    public ResponseEntity<ApiResponse<BackupResponseDTO>> updateBackupStatut(
            @PathVariable Long id,
            @RequestParam BackupLog.StatutBackup statut,
            @RequestParam(required = false) String message) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Statut de la sauvegarde mis à jour", plateformeService.updateBackupStatut(id, statut, message)));
    }

    // ─── §6 — Cibles d'impersonation (liste minimale d'un tenant) ──────────────

    @GetMapping("/entreprises/{id}/utilisateurs")
    public ResponseEntity<ApiResponse<List<UtilisateurCibleDTO>>> utilisateurs(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Utilisateurs de l'entreprise", plateformeService.utilisateurs(id)));
    }

    // ─── §5 — Quotas (global si entrepriseId null) ─────────────────────────────

    @GetMapping("/quotas")
    public ResponseEntity<ApiResponse<List<QuotaResponseDTO>>> quotas() {
        List<QuotaResponseDTO> out = quotaService.findAll().stream()
                .map(q -> new QuotaResponseDTO(q.getEntrepriseId(), null,
                        q.getMaxEmployes(), q.getMaxSucursales(), q.getMaxStockageMo()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(true, "Quotas", out));
    }

    /** Upsert (corps = QuotaResponseDTO ; entrepriseId null = quota global). */
    @PutMapping("/quotas")
    public ResponseEntity<ApiResponse<QuotaResponseDTO>> upsertQuota(
            @RequestBody QuotaResponseDTO dto) {
        Quota q = quotaService.upsert(dto.getEntrepriseId(),
                dto.getMaxEmployes(), dto.getMaxSucursales(), dto.getMaxStockageMo());
        return ResponseEntity.ok(new ApiResponse<>(true, "Quota enregistré",
                new QuotaResponseDTO(q.getEntrepriseId(), null,
                        q.getMaxEmployes(), q.getMaxSucursales(), q.getMaxStockageMo())));
    }

    @DeleteMapping("/quotas/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuota(@PathVariable Long id) {
        quotaService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Quota supprimé", null));
    }
}