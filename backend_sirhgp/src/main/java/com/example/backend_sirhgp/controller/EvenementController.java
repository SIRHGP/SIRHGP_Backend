// ─── EvenementController.java ────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.EvenementRequestDTO;
import com.example.backend_sirhgp.dto.response.EvenementResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.EvenementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CORRECTIONS :
 * - EvenementRequestDTO / EvenementResponseDTO au lieu de EvenementDTO.
 * - @Autowired remplacé par @RequiredArgsConstructor (injection par constructeur).
 * - findAll() paginé.
 */
@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor
public class EvenementController {

    private final EvenementService evenementService;

    @PostMapping
    public ResponseEntity<ApiResponse<EvenementResponseDTO>> create(
            @Valid @RequestBody EvenementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Événement créé avec succès",
                        evenementService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EvenementResponseDTO>>> getAll(
            Pageable pageable,
            @RequestParam(required = false) Long entrepriseId,
            @RequestParam(required = false) Long sucursalId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événements récupérés avec succès",
                        evenementService.findAll(pageable, entrepriseId, sucursalId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvenementResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événement récupéré avec succès",
                        evenementService.findById(id)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<EvenementResponseDTO>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événements récupérés avec succès",
                        evenementService.findByDateRange(start, end)));
    }

    @GetMapping("/participant/{participantId}")
    public ResponseEntity<ApiResponse<List<EvenementResponseDTO>>> getByParticipant(
            @PathVariable Long participantId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événements du participant récupérés",
                        evenementService.findByParticipantId(participantId)));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<ApiResponse<List<EvenementResponseDTO>>> getByProjet(
            @PathVariable Long projetId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événements du projet récupérés",
                        evenementService.findByProjetId(projetId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EvenementResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EvenementRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événement modifié avec succès",
                        evenementService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','LEAD','STAFF','STAGIAIRE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        evenementService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Événement supprimé avec succès", null));
    }
}
