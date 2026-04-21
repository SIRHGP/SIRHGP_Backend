// ─── RoleController.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.RoleRequestDTO;
import com.example.backend_sirhgp.dto.response.RoleResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponseDTO>> create(
            @Valid @RequestBody RoleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Rôle créé avec succès",
                        roleService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoleResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rôles récupérés avec succès",
                        roleService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rôle récupéré avec succès",
                        roleService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Rôle modifié avec succès",
                        roleService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rôle supprimé avec succès", null));
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> addPermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission ajoutée au rôle",
                        roleService.addPermission(roleId, permissionId)));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> removePermission(
            @PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission retirée du rôle",
                        roleService.removePermission(roleId, permissionId)));
    }

    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<ApiResponse<RoleResponseDTO>> replacePermissions(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions du rôle remplacées",
                        roleService.replacePermissions(roleId, permissionIds)));
    }
}