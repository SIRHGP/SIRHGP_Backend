package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Refonte 5.9 (§5) — feature flags EFFECTIFS pour l'utilisateur connecté (tous rôles).
 * Appelé au login côté frontend ( Authcontext / Layout ).
 */
@RestController
@RequestMapping("/api/feature-flags")
@RequiredArgsConstructor
public class PublicFeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> effective() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Feature flags effectifs", featureFlagService.effectiveFlags()));
    }
}