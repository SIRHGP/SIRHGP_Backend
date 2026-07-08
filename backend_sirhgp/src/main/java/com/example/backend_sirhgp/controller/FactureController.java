package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.FactureDispatchRequestDTO;
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

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {

    private final FactureService factureService;

    // ─── CRUD de base ─────────────────────────────────────────────────────────

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

    // ─── DISPATCH (ENVOI AU CLIENT) ───────────────────────────────────────────

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> dispatch(
            @PathVariable Long id,
            @Valid @RequestBody FactureDispatchRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Facture envoyée au client avec succès",
                        factureService.dispatchToClient(id, dto)));
    }

    @PatchMapping("/{id}/consult")
    public ResponseEntity<ApiResponse<Void>> markAsConsulted(@PathVariable Long id) {
        factureService.markAsConsulted(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Facture marquée comme consultée", null));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<FactureResponseDTO>>> getReceivedFactures() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures reçues récupérées",
                        factureService.getReceivedFactures()));
    }

    @GetMapping("/unconsulted")
    public ResponseEntity<ApiResponse<List<FactureResponseDTO>>> getUnconsultedFactures() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures non consultées récupérées",
                        factureService.getUnconsultedFactures()));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<FactureResponseDTO>>> getSentFactures() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Factures envoyées récupérées",
                        factureService.getSentFactures()));
    }
}