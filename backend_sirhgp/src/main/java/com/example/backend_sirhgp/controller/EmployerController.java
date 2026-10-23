package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.request.EmployerUpdateDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.dto.response.EmployerStatsDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.EmployerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CORRECTIONS :
 * - EmployerCreateDTO / EmployerUpdateDTO / EmployerResponseDTO au lieu de EmployerDTO.
 * - Noms de méthodes service alignés (create, findAll, findById, update, delete, toggleActif, getStats).
 * - findAll() paginé.
 * - Endpoint changePassword ajouté.
 * - BindingResult supprimé.
 */
@RestController
@RequestMapping("/api/employers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
public class EmployerController {

    private final EmployerService employerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> create(
            @Valid @RequestBody EmployerCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Employé créé avec succès",
                        employerService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployerResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employés récupérés avec succès",
                        employerService.findAll(pageable)));
    }

    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<List<EmployerResponseDTO>>> getLeads() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Responsables récupérés", employerService.findLeads()));
    }

    @GetMapping("/organigramme")
    public ResponseEntity<ApiResponse<List<EmployerResponseDTO>>> getOrganigramme() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Organigramme récupéré", employerService.findOrganigramme()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employé récupéré avec succès",
                        employerService.findById(id)));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<EmployerStatsDTO>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statistiques récupérées avec succès",
                        employerService.getStats(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployerUpdateDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employé modifié avec succès",
                        employerService.update(id, dto)));
    }

    /**
     * Endpoint dédié au changement de mot de passe.
     * PATCH /api/employers/{id}/change-password
     */
    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestParam String ancienMotDePasse,
            @RequestParam String nouveauMotDePasse) {
        employerService.changePassword(id, ancienMotDePasse, nouveauMotDePasse);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mot de passe modifié avec succès", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employerService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Employé supprimé avec succès", null));
    }

    @PatchMapping("/{id}/toggle-actif")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<EmployerResponseDTO>> toggleActif(@PathVariable Long id) {
        EmployerResponseDTO updated = employerService.toggleActif(id);
        String msg = updated.isActif() ? "Employé activé" : "Employé désactivé";
        return ResponseEntity.ok(new ApiResponse<>(true, msg, updated));
    }
}
