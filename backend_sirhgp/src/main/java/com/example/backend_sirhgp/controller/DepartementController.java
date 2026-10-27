// ─── DepartementController.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.DepartementRequestDTO;
import com.example.backend_sirhgp.dto.response.DepartementResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.DepartementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DepartementController {

    private final DepartementService departementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DepartementResponseDTO>> create(
            @Valid @RequestBody DepartementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Département créé avec succès",
                        departementService.create(dto)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<Page<DepartementResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Départements récupérés avec succès",
                        departementService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<DepartementResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Département récupéré avec succès",
                        departementService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartementResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartementRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Département modifié avec succès",
                        departementService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        departementService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Département supprimé avec succès", null));
    }

    @PostMapping("/{departementId}/postes/{posteId}")
    public ResponseEntity<ApiResponse<DepartementResponseDTO>> addPoste(
            @PathVariable Long departementId, @PathVariable Long posteId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste ajouté au département",
                        departementService.addPoste(departementId, posteId)));
    }

    @DeleteMapping("/{departementId}/postes/{posteId}")
    public ResponseEntity<ApiResponse<DepartementResponseDTO>> removePoste(
            @PathVariable Long departementId, @PathVariable Long posteId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste retiré du département",
                        departementService.removePoste(departementId, posteId)));
    }
}
