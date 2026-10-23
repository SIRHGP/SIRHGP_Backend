// ─── PointageController.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.response.PointageResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.PointageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Pointage (badgeuse) — 4.1. Rôles RH uniquement (5.9 §7.2 : SUPER_ADMIN retiré) :
 * chacun pointe ses propres heures (arrivée / départ, 5j sur 7).
 */
@RestController
@RequestMapping("/api/pointages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAGIAIRE', 'STAFF', 'LEAD', 'MANAGER', 'ADMIN', 'SECRETAIRE')")
public class PointageController {

    private final PointageService pointageService;

    /** 1er clic du jour : enregistre l'heure d'arrivée. */
    @PostMapping("/clock-in")
    public ResponseEntity<ApiResponse<PointageResponseDTO>> clockIn() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Arrivée enregistrée", pointageService.clockIn()));
    }

    /** 2e clic du jour : enregistre l'heure de départ et calcule la durée. */
    @PostMapping("/clock-out")
    public ResponseEntity<ApiResponse<PointageResponseDTO>> clockOut() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Départ enregistré", pointageService.clockOut()));
    }

    /** Rattrapage : saisie manuelle de l'heure de départ oubliée (pointage marqué « corrigé »). */
    @PatchMapping("/{id}/depart")
    public ResponseEntity<ApiResponse<PointageResponseDTO>> corrigerDepart(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime heure) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Heure de départ corrigée", pointageService.corrigerDepart(id, heure)));
    }

    /** État du jour : pointage d'aujourd'hui (null si pas encore pointé). */
    @GetMapping("/aujourdhui")
    public ResponseEntity<ApiResponse<PointageResponseDTO>> aujourdhui() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Pointage du jour", pointageService.aujourdhui()));
    }

    /** Historique de la période (jour / semaine / mois) de l'utilisateur courant. */
    @GetMapping("/periode")
    public ResponseEntity<ApiResponse<List<PointageResponseDTO>>> periode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Pointages de la période", pointageService.periode(debut, fin)));
    }
}
