package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * CORRECTIONS :
 * - FactureRequestDTO / FactureResponseDTO au lieu de FactureDTO.
 * - @Autowired remplacé par @RequiredArgsConstructor.
 * - enregistrerPaiement() reçoit BigDecimal au lieu de Double.
 * - findByProjet() → findByProjetId() (renommé dans le service).
 * - try/catch supprimés — GlobalExceptionHandler gère tout.
 * - findAll() paginé.
 */
@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;

    @PostMapping
    public ResponseEntity<ApiResponse<FactureResponseDTO>> create(
            @Valid @RequestBody FactureRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Facture créée avec succès",
                        factureService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FactureResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures récupérées avec succès",
                        factureService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Facture récupérée avec succès",
                        factureService.findById(id)));
    }

    // CORRECTION : findByProjetId (renommé dans le service)
    @GetMapping("/by-projet/{projetId}")
    public ResponseEntity<ApiResponse<List<FactureResponseDTO>>> getByProjet(
            @PathVariable Long projetId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures du projet récupérées",
                        factureService.findByProjetId(projetId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody FactureRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Facture modifiée avec succès",
                        factureService.update(id, dto)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> updateStatut(
            @PathVariable Long id,
            @RequestParam Facture.StatutFacture statut) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Statut de la facture mis à jour",
                        factureService.updateStatut(id, statut)));
    }

    // CORRECTION : BigDecimal au lieu de Double
    @PatchMapping("/{id}/paiement")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> enregistrerPaiement(
            @PathVariable Long id,
            @RequestParam BigDecimal montant) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Paiement enregistré avec succès",
                        factureService.enregistrerPaiement(id, montant)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        factureService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Facture supprimée avec succès", null));
    }
}