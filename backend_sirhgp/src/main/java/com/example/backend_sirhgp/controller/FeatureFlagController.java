package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.response.FeatureFlagResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Refonte 5.9 (§5) — feature flags (gestion SUPER_ADMIN).
 * La résolution effective pour l'utilisateur connecté est sur GET /api/feature-flags
 * (voir PublicFeatureFlagController).
 */
@RestController
@RequestMapping("/api/plateforme/feature-flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeatureFlagResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Feature flags", featureFlagService.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeatureFlagResponseDTO>> create(
            @RequestParam String cle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") boolean actifParDefaut) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true,
                "Feature flag créé", featureFlagService.create(cle, description, actifParDefaut)));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<FeatureFlagResponseDTO>> update(
            @PathVariable String key,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean actifParDefaut) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Feature flag modifié", featureFlagService.update(key, description, actifParDefaut)));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String key) {
        featureFlagService.delete(key);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feature flag supprimé", null));
    }

    /** Override par tenant : activer/désactiver le flag pour une entreprise. */
    @PatchMapping("/{key}/tenants/{entrepriseId}")
    public ResponseEntity<ApiResponse<FeatureFlagResponseDTO>> overrideTenant(
            @PathVariable String key,
            @PathVariable Long entrepriseId,
            @RequestParam boolean actif) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Override enregistré", featureFlagService.overrideTenant(key, entrepriseId, actif)));
    }
}