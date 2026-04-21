package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.dashboard.ProjetDashboardDTO;
import com.example.backend_sirhgp.dto.request.ProjetRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FactureService;
import com.example.backend_sirhgp.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CORRECTIONS :
 * - ProjetRequestDTO / ProjetResponseDTO au lieu de ProjetDTO.
 * - findAll() paginé.
 * - factureService.findByProjet() → findByProjetId() (renommé dans le service).
 * - try/catch supprimés — GlobalExceptionHandler gère tout.
 * - Dashboard stubs conservés — à compléter quand le service sera enrichi.
 */
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;
    private final FactureService factureService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> create(
            @Valid @RequestBody ProjetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Projet créé avec succès",
                        projetService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjetResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projets récupérés avec succès",
                        projetService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projet récupéré avec succès",
                        projetService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjetRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projet modifié avec succès",
                        projetService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        projetService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projet supprimé avec succès", null));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> updateStatut(
            @PathVariable Long id,
            @RequestParam Projet.StatutProjet statut) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statut du projet mis à jour",
                        projetService.updateStatut(id, statut)));
    }

    // ─── Membres ─────────────────────────────────────────────

    @PostMapping("/{projetId}/membres/{membreId}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> addMembre(
            @PathVariable Long projetId, @PathVariable Long membreId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Membre ajouté au projet",
                        projetService.addMembre(projetId, membreId)));
    }

    @DeleteMapping("/{projetId}/membres/{membreId}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> removeMembre(
            @PathVariable Long projetId, @PathVariable Long membreId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Membre retiré du projet",
                        projetService.removeMembre(projetId, membreId)));
    }

    // ─── Départements ────────────────────────────────────────

    @PostMapping("/{projetId}/departements/{departementId}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> addDepartement(
            @PathVariable Long projetId, @PathVariable Long departementId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Département ajouté au projet",
                        projetService.addDepartement(projetId, departementId)));
    }

    @DeleteMapping("/{projetId}/departements/{departementId}")
    public ResponseEntity<ApiResponse<ProjetResponseDTO>> removeDepartement(
            @PathVariable Long projetId, @PathVariable Long departementId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Département retiré du projet",
                        projetService.removeDepartement(projetId, departementId)));
    }

    @GetMapping("/by-departement/{departementId}")
    public ResponseEntity<ApiResponse<List<ProjetResponseDTO>>> getByDepartement(
            @PathVariable Long departementId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projets du département récupérés",
                        projetService.findByDepartement(departementId)));
    }

    // ─── Factures du projet ───────────────────────────────────

    // CORRECTION : factureService.findByProjetId() au lieu de findByProjet()
    @GetMapping("/{projetId}/factures")
    public ResponseEntity<ApiResponse<List<FactureResponseDTO>>> getFactures(
            @PathVariable Long projetId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures du projet récupérées",
                        factureService.findByProjetId(projetId)));
    }

    // ─── Dashboard ───────────────────────────────────────────

    @GetMapping("/dashboard/global")
    public ResponseEntity<ApiResponse<ProjetDashboardDTO>> getGlobalDashboard() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statistiques globales récupérées",
                        projetService.getGlobalDashboardStats()));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<ProjetDashboardDTO.ProjetDetailView>> getProjetDashboard(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Dashboard du projet récupéré",
                        projetService.getProjetDashboardStats(id)));
    }

    @GetMapping("/statistiques/statut")
    public ResponseEntity<ApiResponse<List<ProjetDashboardDTO.StatutStatDTO>>> getStatutStats() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statistiques par statut récupérées",
                        projetService.getStatistiquesParStatut()));
    }

    @GetMapping("/statistiques/budget")
    public ResponseEntity<ApiResponse<ProjetDashboardDTO.BudgetGlobalDTO>> getBudgetStats() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statistiques budgétaires récupérées",
                        projetService.getStatistiquesBudget()));
    }

    @GetMapping("/en-retard")
    public ResponseEntity<ApiResponse<List<ProjetResponseDTO>>> getProjetsEnRetard() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Projets en retard récupérés",
                        projetService.findProjetsEnRetard()));
    }
}