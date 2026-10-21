package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.DemandePermissionRequestDTO;
import com.example.backend_sirhgp.dto.response.DemandePermissionResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.DemandePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Refonte 5.6 — Demandes de permission (Timesheet).
 * POST          /api/permissions             → soumettre (STAFF/STAGIAIRE/LEAD/MANAGER/ADMIN)
 * GET           /api/permissions/mes         → « Mes demandes »
 * GET           /api/permissions/a-valider   → file du valideur connecté
 * PATCH         /api/permissions/{id}/decision?statut=APPROUVEE|REJETEE&commentaire=…
 * PATCH         /api/permissions/{id}/annuler
 */
@RestController
@RequestMapping("/api/demandes-permission")
@RequiredArgsConstructor
public class DemandePermissionController {

    private final DemandePermissionService service;

    @PostMapping
    public ResponseEntity<ApiResponse<DemandePermissionResponseDTO>> create(
            @Valid @RequestBody DemandePermissionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Demande de permission soumise avec succès", service.create(dto)));
    }

    @GetMapping("/mes")
    public ResponseEntity<ApiResponse<List<DemandePermissionResponseDTO>>> mesDemandes() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Mes demandes récupérées", service.mesDemandes()));
    }

    @GetMapping("/a-valider")
    public ResponseEntity<ApiResponse<List<DemandePermissionResponseDTO>>> aValider() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Demandes à valider récupérées", service.aValider()));
    }

    @PatchMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<DemandePermissionResponseDTO>> decider(
            @PathVariable Long id,
            @RequestParam String statut,
            @RequestParam(required = false) String commentaire) {
        String message = "APPROUVEE".equals(statut) ? "Permission approuvée" : "Permission rejetée";
        return ResponseEntity.ok(new ApiResponse<>(true, message, service.decider(id, statut, commentaire)));
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<DemandePermissionResponseDTO>> annuler(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Demande annulée", service.annuler(id)));
    }
}
