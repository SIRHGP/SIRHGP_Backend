// ─── FeuilleTempsController.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.FeuilleTempsRequestDTO;
import com.example.backend_sirhgp.dto.response.FeuilleTempsResponseDTO;
import com.example.backend_sirhgp.model.FeuilleTemps;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FeuilleTempsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timesheet")
@RequiredArgsConstructor
// 5.9 (§7.2) : rôles RH uniquement — SUPER_ADMIN retiré.
@PreAuthorize("hasAnyRole('STAGIAIRE', 'STAFF', 'LEAD', 'MANAGER', 'ADMIN')")
public class FeuilleTempsController {

    private final FeuilleTempsService feuilleService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeuilleTempsResponseDTO>> create(@Valid @RequestBody FeuilleTempsRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Heures enregistrées", feuilleService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeuilleTempsResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Feuilles récupérées", feuilleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeuilleTempsResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Feuille récupérée", feuilleService.findById(id)));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<ApiResponse<List<FeuilleTempsResponseDTO>>> byEmploye(@PathVariable Long employeId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Feuilles de l'employé", feuilleService.findByEmployeId(employeId)));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<ApiResponse<List<FeuilleTempsResponseDTO>>> byStatut(@PathVariable FeuilleTemps.StatutFeuille statut) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Feuilles par statut", feuilleService.findByStatut(statut)));
    }

    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> total(
            @RequestParam Long employeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Total d'heures", feuilleService.totalHeures(employeId, debut, fin)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<FeuilleTempsResponseDTO>> updateStatut(
            @PathVariable Long id, @RequestParam FeuilleTemps.StatutFeuille statut) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Statut de la feuille mis à jour",
                feuilleService.updateStatut(id, statut)));
    }
}