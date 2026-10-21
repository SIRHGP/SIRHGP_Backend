package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.FacturationCycleRequestDTO;
import com.example.backend_sirhgp.dto.request.FacturationDocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.PaiementFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FacturationCycleResponseDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.dto.response.PaiementFactureResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.FacturationCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Dossier de facturation (refonte interface) — cycle séquentiel en 3 étapes :
 * 1) PROFORMA (PDF reçu) → 2) BON DE COMMANDE (saisie + PDF généré)
 * → 3) FACTURE RÉGULIÈRE (après paiement) → clôture.
 */
@RestController
@RequestMapping("/api/facturation-cycles")
@RequiredArgsConstructor
public class FacturationCycleController {

    private final FacturationCycleService cycleService;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> create(
            @Valid @RequestBody FacturationCycleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Dossier de facturation ouvert", cycleService.create(dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacturationCycleResponseDTO>>> getAll() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Dossiers de facturation récupérés", cycleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Dossier récupéré", cycleService.findById(id)));
    }

    // ─── Étape 1 — PROFORMA ───────────────────────────────────────────────────

    /** Étape 1 : upload du PDF du proforma (multipart, PDF uniquement). */
    @PostMapping("/{id}/proforma")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> uploadProforma(
            @PathVariable Long id,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Proforma reçu — étape 1/3 validée", cycleService.uploadProforma(id, fichier)));
    }

    // ─── Étape 2 — BON DE COMMANDE ────────────────────────────────────────────

    /** Étape 2 : génération du bon de commande depuis les champs de saisie. */
    @PostMapping("/{id}/bon-commande")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> genererBonCommande(
            @PathVariable Long id,
            @Valid @RequestBody FacturationDocumentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Bon de commande généré — étape 2/3 validée",
                        cycleService.genererBonCommande(id, dto)));
    }

    /** PDF OHADA du bon de commande (OpenPDF). */
    @GetMapping("/{id}/bon-commande/pdf")
    public ResponseEntity<byte[]> pdfBonCommande(@PathVariable Long id) {
        byte[] pdf = cycleService.pdfBonCommande(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bon-de-commande.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── Étape 3 — FACTURE RÉGULIÈRE ──────────────────────────────────────────

    /** Étape 3 (saisie) : facture régulière après paiement, chaînée au BC. */
    @PostMapping("/{id}/facture-finale")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> creerFactureFinale(
            @PathVariable Long id,
            @Valid @RequestBody FacturationDocumentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Facture régulière enregistrée — étape 3/3 validée",
                        cycleService.creerFactureFinale(id, dto)));
    }

    /** Étape 3 (alternative) : réception du PDF de la facture finale. */
    @PostMapping("/{id}/facture-finale/pdf")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> uploadFactureFinalePdf(
            @PathVariable Long id,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Facture finale reçue — étape 3/3 validée", cycleService.uploadFactureFinalePdf(id, fichier)));
    }

    /** Encaissement de la facture régulière (écriture trésorerie OHADA). */
    @PostMapping("/{id}/paiements")
    public ResponseEntity<ApiResponse<FactureResponseDTO>> enregistrerPaiement(
            @PathVariable Long id,
            @Valid @RequestBody PaiementFactureRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Règlement enregistré", cycleService.enregistrerPaiement(id, dto)));
    }

    @GetMapping("/{id}/paiements")
    public ResponseEntity<ApiResponse<List<PaiementFactureResponseDTO>>> getPaiements(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Règlements récupérés", cycleService.getPaiements(id)));
    }

    // ─── Clôture / suppression ────────────────────────────────────────────────

    /** Clôture du cycle (3 étapes complétées + solde réglé si régulière saisie). */
    @PostMapping("/{id}/cloturer")
    public ResponseEntity<ApiResponse<FacturationCycleResponseDTO>> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dossier clôturé", cycleService.cloturer(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cycleService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dossier supprimé", null));
    }
}