// ─── ActionController.java ───────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.ActionRequestDTO;
import com.example.backend_sirhgp.dto.response.ActionResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.ActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CORRECTIONS :
 * - ActionRequestDTO / ActionResponseDTO au lieu de l'ancien ActionDTO.
 * - Noms de méthodes service alignés sur les nouveaux (create, findAll, findById, update, delete).
 * - findAll() paginé.
 * - BindingResult supprimé — GlobalExceptionHandler gère la validation.
 */
@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ActionController {

    private final ActionService actionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ActionResponseDTO>> create(
            @Valid @RequestBody ActionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Action créée avec succès",
                        actionService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ActionResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Actions récupérées avec succès",
                        actionService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActionResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Action récupérée avec succès",
                        actionService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ActionResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody ActionRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Action modifiée avec succès",
                        actionService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        actionService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Action supprimée avec succès", null));
    }
}