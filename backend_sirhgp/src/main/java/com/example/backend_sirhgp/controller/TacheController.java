package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.TacheRequestDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.TacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CORRECTIONS :
 * - TacheRequestDTO / TacheResponseDTO au lieu de TacheDTO.
 * - findAll() paginé.
 */
@RestController
@RequestMapping("/api/taches")
@RequiredArgsConstructor
public class TacheController {

    private final TacheService tacheService;

    @PostMapping
    public ResponseEntity<ApiResponse<TacheResponseDTO>> create(
            @Valid @RequestBody TacheRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tâche créée avec succès",
                        tacheService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TacheResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâches récupérées avec succès",
                        tacheService.findAll(pageable)));
    }

    @GetMapping("/mon-etat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> monEtat() {
        return ResponseEntity.ok(new ApiResponse<>(true, "État courant", tacheService.getMonEtat()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TacheResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâche récupérée avec succès",
                        tacheService.findById(id)));
    }

    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<ApiResponse<List<TacheResponseDTO>>> getByAssignee(
            @PathVariable Long assigneeId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâches de l'assignee récupérées",
                        tacheService.findByAssigneeId(assigneeId)));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<ApiResponse<List<TacheResponseDTO>>> getByProjet(
            @PathVariable Long projetId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâches du projet récupérées",
                        tacheService.findByProjetId(projetId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TacheResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody TacheRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâche modifiée avec succès",
                        tacheService.update(id, dto)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<TacheResponseDTO>> updateStatut(
            @PathVariable Long id,
            @RequestParam Tache.StatutTache statut) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statut de la tâche mis à jour",
                        tacheService.updateStatut(id, statut)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','LEAD','STAFF','STAGIAIRE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tacheService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Tâche supprimée avec succès", null));
    }
}
