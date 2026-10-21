// ─── EvaluationController.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.EvaluationRequestDTO;
import com.example.backend_sirhgp.dto.response.EvaluationResponseDTO;
import com.example.backend_sirhgp.model.Evaluation;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public ResponseEntity<ApiResponse<EvaluationResponseDTO>> create(@Valid @RequestBody EvaluationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Évaluation créée", evaluationService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvaluationResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Évaluations récupérées", evaluationService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluationResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Évaluation récupérée", evaluationService.findById(id)));
    }

    /**
     * Refonte 6.1 — KPI de l'évalué (tâches + assiduité) pour le formulaire
     * d'évaluation et le rapport.
     */
    @GetMapping("/kpi/{employeId}")
    public ResponseEntity<ApiResponse<com.example.backend_sirhgp.dto.response.EvaluationKpiDTO>> kpi(
            @PathVariable Long employeId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "KPI récupérés",
                evaluationService.kpi(employeId)));
    }

    @GetMapping("/evalue/{evalueId}")
    public ResponseEntity<ApiResponse<List<EvaluationResponseDTO>>> byEvalue(@PathVariable Long evalueId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Évaluations de l'employé",
                evaluationService.findByEvalue(evalueId)));
    }

    @GetMapping("/evaluateur/{evaluateurId}")
    public ResponseEntity<ApiResponse<List<EvaluationResponseDTO>>> byEvaluateur(@PathVariable Long evaluateurId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Évaluations de l'évaluateur",
                evaluationService.findByEvaluateur(evaluateurId)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<EvaluationResponseDTO>> updateStatut(
            @PathVariable Long id, @RequestParam Evaluation.StatutEvaluation statut) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Statut de l'évaluation mis à jour",
                evaluationService.updateStatut(id, statut)));
    }
}