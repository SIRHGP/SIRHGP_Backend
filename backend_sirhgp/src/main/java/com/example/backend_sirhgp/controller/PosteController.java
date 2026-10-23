package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.PosteRequestDTO;
import com.example.backend_sirhgp.dto.response.PosteResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.PosteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * CORRECTIONS :
 * - PosteRequestDTO / PosteResponseDTO au lieu de PosteDTO.
 * - findByRole() supprimé — n'existe pas dans PosteService corrigé.
 *   (à ajouter dans PosteRepository/Service si le besoin existe).
 * - Noms de méthodes service alignés (addPermission, removePermission, etc.).
 */
@RestController
@RequestMapping("/api/postes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class PosteController {

    private final PosteService posteService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Statistiques des postes", posteService.getStats()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PosteResponseDTO>> create(
            @Valid @RequestBody PosteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Poste créé avec succès",
                        posteService.create(dto)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<Page<PosteResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Postes récupérés avec succès",
                        posteService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste récupéré avec succès",
                        posteService.findById(id)));
    }

    @GetMapping("/by-departement/{departementId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<List<PosteResponseDTO>>> getByDepartement(
            @PathVariable Long departementId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Postes du département récupérés avec succès",
                        posteService.findByDepartement(departementId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody PosteRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste modifié avec succès",
                        posteService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        posteService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste supprimé avec succès", null));
    }

    @PostMapping("/{posteId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> addPermission(
            @PathVariable Long posteId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission ajoutée au poste",
                        posteService.addPermission(posteId, permissionId)));
    }

    @DeleteMapping("/{posteId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> removePermission(
            @PathVariable Long posteId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission retirée du poste",
                        posteService.removePermission(posteId, permissionId)));
    }

    @PutMapping("/{posteId}/permissions")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> replacePermissions(
            @PathVariable Long posteId,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions du poste remplacées",
                        posteService.replacePermissions(posteId, permissionIds)));
    }

    @PostMapping("/{posteId}/assign-departement/{departementId}")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> assignToDepartement(
            @PathVariable Long posteId, @PathVariable Long departementId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste assigné au département",
                        posteService.assignToDepartement(posteId, departementId)));
    }

    @DeleteMapping("/{posteId}/remove-departement")
    public ResponseEntity<ApiResponse<PosteResponseDTO>> removeFromDepartement(
            @PathVariable Long posteId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Poste retiré du département",
                        posteService.removeFromDepartement(posteId)));
    }
}
