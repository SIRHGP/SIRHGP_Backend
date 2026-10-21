// ─── OffreEmploiController.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.OffreEmploiRequestDTO;
import com.example.backend_sirhgp.dto.response.OffreEmploiResponseDTO;
import com.example.backend_sirhgp.model.OffreEmploi;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.OffreEmploiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/offres")
@RequiredArgsConstructor
public class OffreEmploiController {

    private final OffreEmploiService offreService;

    // Refonte LEAD 6.0 : le LEAD peut piloter le recrutement de SON département
    // (offre → candidature → embauche). Le 4e verbe est indissociable : une offre
    // que seul l'ADMIN peut clôturer resterait indéfiniment ouverte. Le périmètre
    // départemental est vérifié dans le service (LeadPerimetreService), pas ici.
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD')")
    public ResponseEntity<ApiResponse<OffreEmploiResponseDTO>> create(
            @Valid @RequestBody OffreEmploiRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Offre créée avec succès", offreService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OffreEmploiResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Offres récupérées avec succès", offreService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OffreEmploiResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Offre récupérée avec succès", offreService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD')")
    public ResponseEntity<ApiResponse<OffreEmploiResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody OffreEmploiRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Offre modifiée avec succès", offreService.update(id, dto)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD')")
    public ResponseEntity<ApiResponse<OffreEmploiResponseDTO>> updateStatut(
            @PathVariable Long id, @RequestParam OffreEmploi.StatutOffre statut) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Statut de l'offre mis à jour",
                offreService.updateStatut(id, statut)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEAD')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        offreService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Offre supprimée avec succès", null));
    }
}