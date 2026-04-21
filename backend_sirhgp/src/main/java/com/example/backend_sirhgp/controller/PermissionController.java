package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.PermissionRequestDTO;
import com.example.backend_sirhgp.dto.response.PermissionResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> create(
            @Valid @RequestBody PermissionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Permission créée avec succès",
                        permissionService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PermissionResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<PermissionResponseDTO> permissions = permissionService.findAll(pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions récupérées avec succès", permissions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission récupérée avec succès",
                        permissionService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission modifiée avec succès",
                        permissionService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permission supprimée avec succès", null));
    }

    @PostMapping("/{permissionId}/actions/{actionId}")
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> addAction(
            @PathVariable Long permissionId, @PathVariable Long actionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Action ajoutée à la permission",
                        permissionService.addAction(permissionId, actionId)));
    }

    @DeleteMapping("/{permissionId}/actions/{actionId}")
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> removeAction(
            @PathVariable Long permissionId, @PathVariable Long actionId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Action retirée de la permission",
                        permissionService.removeAction(permissionId, actionId)));
    }

    @PutMapping("/{permissionId}/actions")
    public ResponseEntity<ApiResponse<PermissionResponseDTO>> replaceActions(
            @PathVariable Long permissionId,
            @RequestBody Set<Long> actionIds) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Actions de la permission remplacées",
                        permissionService.replaceActions(permissionId, actionIds)));
    }
}