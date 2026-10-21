// ─── EntrepriseController.java ───────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.dashboard.SuperAdminStatsDTO;
import com.example.backend_sirhgp.dto.request.EntrepriseRequestDTO;
import com.example.backend_sirhgp.dto.request.PointageConfigDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.dto.response.EntrepriseResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.EntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de gestion des entreprises (multi-tenant).
 * - Lecture : SUPER_ADMIN (toutes) ou ADMIN (la sienne — filtrage via le service/contexte).
 * - Écriture : SUPER_ADMIN uniquement.
 */
@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EntrepriseResponseDTO>> create(
            @Valid @RequestBody EntrepriseRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Entreprise créée avec succès",
                        entrepriseService.create(dto)));
    }

    // ─── Super Admin : statistiques transverses ──────────────────────────────
    @GetMapping("/super-admin/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SuperAdminStatsDTO>> getSuperAdminStats() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Statistiques Super Admin récupérées",
                entrepriseService.getSuperAdminStats()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EntrepriseResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Entreprises récupérées avec succès",
                        entrepriseService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EntrepriseResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Entreprise récupérée avec succès",
                        entrepriseService.findById(id)));
    }

    // ─── Super Admin : admins d'une entreprise (3.2) ─────────────────────────
    @GetMapping("/{id}/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployerResponseDTO>>> getAdmins(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Admins de l'entreprise récupérés avec succès",
                        entrepriseService.getAdmins(id)));
    }

    @PatchMapping("/{id}/admins/{adminId}/statut")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> toggleAdminStatut(
            @PathVariable Long id, @PathVariable Long adminId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statut de l'admin modifié",
                        entrepriseService.toggleAdminActif(id, adminId)));
    }

    // ─── Refonte 5.9 (§2) — gestion des comptes ADMIN ────────────────────────

    /** Création d'un compte ADMIN supplémentaire (rôle forcé ADMIN côté service). */
    @PostMapping("/{id}/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> createAdmin(
            @PathVariable Long id, @Valid @RequestBody com.example.backend_sirhgp.dto.request.AdminCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
                "Compte admin créé", entrepriseService.createAdmin(id, dto)));
    }

    /** Promouvoir / rétrograder un compte (jamais le dernier admin actif, jamais SUPER_ADMIN). */
    @PatchMapping("/{id}/admins/{adminId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> changeAdminRole(
            @PathVariable Long id, @PathVariable Long adminId, @RequestParam String role) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Rôle du compte mis à jour", entrepriseService.changeAdminRole(id, adminId, role)));
    }

    /** Réinitialisation : mot de passe temporaire renvoyé hors audit (drapeau levé). */
    @PostMapping("/{id}/admins/{adminId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.example.backend_sirhgp.dto.response.ResetPasswordResponseDTO>> resetAdminPassword(
            @PathVariable Long id, @PathVariable Long adminId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Mot de passe réinitialisé", entrepriseService.resetAdminPassword(id, adminId)));
    }

    // ─── Configuration du pointage (4.2, ADMIN ou SUPER_ADMIN) ─────────────────
    @PatchMapping("/{id}/pointage-config")
    public ResponseEntity<ApiResponse<EntrepriseResponseDTO>> updatePointageConfig(
            @PathVariable Long id, @Valid @RequestBody PointageConfigDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Configuration du pointage enregistrée",
                entrepriseService.updatePointageConfig(id, dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EntrepriseResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EntrepriseRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Entreprise modifiée avec succès",
                        entrepriseService.update(id, dto)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<EntrepriseResponseDTO>> toggleStatut(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statut de l'entreprise modifié",
                        entrepriseService.toggleStatut(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Entreprise supprimée avec succès", null));
    }
}