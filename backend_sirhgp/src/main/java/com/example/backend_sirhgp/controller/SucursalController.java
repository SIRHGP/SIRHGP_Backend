// ─── SucursalController.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.SucursalRequestDTO;
import com.example.backend_sirhgp.dto.response.SucursalResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.SucursalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API des sucursales (3.4) — refonte 5.9 :
 * - lecture : SUPER_ADMIN (transverse), ADMIN (son entreprise), MANAGER/LEAD (leur sucursal,
 *   scoping dans le service) ;
 * - écritures + affectation manager : ADMIN de l'entreprise SEUL (décision 0.1).
 */
@RestController
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
public class SucursalController {

    private final SucursalService sucursalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SucursalResponseDTO>>> getAll(
            @RequestParam(required = false) Long entrepriseId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Sucursales récupérées", sucursalService.findAll(entrepriseId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SucursalResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Sucursal récupérée", sucursalService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponseDTO>> create(
            @RequestParam Long entrepriseId, @Valid @RequestBody SucursalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
                "Sucursal créée", sucursalService.create(entrepriseId, dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody SucursalRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Sucursal modifiée", sucursalService.update(id, dto)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponseDTO>> toggleStatut(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Statut de la sucursal modifié", sucursalService.toggleStatut(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sucursalService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Sucursal supprimée", null));
    }

    /**
     * 5.9 (§7) — affectation / désaffectation du manager de la sucursal.
     * managerId absent = désaffectation. ADMIN uniquement.
     */
    @PatchMapping("/{id}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SucursalResponseDTO>> assignManager(
            @PathVariable Long id, @RequestParam(required = false) Long managerId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                managerId != null ? "Manager affecté à la sucursal" : "Manager retiré de la sucursal",
                sucursalService.assignManager(id, managerId)));
    }
}