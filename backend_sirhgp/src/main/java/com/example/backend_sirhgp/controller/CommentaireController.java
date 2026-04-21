package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.CommentaireRequestDTO;
import com.example.backend_sirhgp.dto.response.CommentaireResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.CommentaireService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CORRECTIONS :
 * - CommentaireRequestDTO en entrée, CommentaireResponseDTO en sortie.
 * - update() : reçoit un @RequestBody String contenu au lieu de CommentaireDTO
 *   (le service attend un String depuis la refacto).
 * - BindingResult supprimé — GlobalExceptionHandler gère la validation.
 */
@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {

    private final CommentaireService commentaireService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentaireResponseDTO>> create(
            @Valid @RequestBody CommentaireRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Commentaire créé avec succès",
                        commentaireService.create(dto)));
    }

    @GetMapping("/tache/{tacheId}")
    public ResponseEntity<ApiResponse<List<CommentaireResponseDTO>>> getByTache(
            @PathVariable Long tacheId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Commentaires récupérés avec succès",
                        commentaireService.findByTacheId(tacheId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentaireResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Commentaire récupéré avec succès",
                        commentaireService.findById(id)));
    }

    /**
     * CORRECTION : update() reçoit le nouveau contenu directement (String),
     * pas un CommentaireDTO complet. Seul le contenu est modifiable.
     * PUT /api/commentaires/{id}
     * Body : "Mon nouveau contenu"
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentaireResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody @NotBlank String contenu) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Commentaire modifié avec succès",
                        commentaireService.update(id, contenu)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        commentaireService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Commentaire supprimé avec succès", null));
    }

    @DeleteMapping("/tache/{tacheId}")
    public ResponseEntity<ApiResponse<Void>> deleteByTache(@PathVariable Long tacheId) {
        commentaireService.deleteByTacheId(tacheId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Commentaires de la tâche supprimés", null));
    }
}