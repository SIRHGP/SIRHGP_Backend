package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.DocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.DocumentTransferRequestDTO;
import com.example.backend_sirhgp.dto.response.DocumentPartageResponseDTO;
import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ── Upload ───────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> upload(
            @RequestPart("document") @Valid DocumentRequestDTO dto,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(201).body(
                new ApiResponse<>(true, "Document uploadé avec succès",
                        documentService.upload(dto, fichier)));
    }

    // ── Transfer ─────────────────────────────────────────────────────────────

    /**
     * POST /api/documents/transfer/{id}
     * Transfère un document existant à un autre employé.
     * Seuls l'expéditeur et le destinataire auront accès.
     */
    @PostMapping("/transfer/{id}")
    public ResponseEntity<ApiResponse<DocumentPartageResponseDTO>> transfer(
            @PathVariable Long id,
            @Valid @RequestBody DocumentTransferRequestDTO dto) {
        return ResponseEntity.status(201).body(
                new ApiResponse<>(true, "Document transféré avec succès",
                        documentService.transfer(id, dto)));
    }

    // ── Partages reçus ───────────────────────────────────────────────────────

    /**
     * GET /api/documents/partages/recus/{employerId}
     * Documents reçus par un employé — visibles uniquement par lui.
     */
    @GetMapping("/partages/recus/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentPartageResponseDTO>>> getPartagesRecus(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Partages reçus récupérés",
                        documentService.getPartagesRecus(employerId)));
    }

    // ── Partages envoyés ─────────────────────────────────────────────────────

    /**
     * GET /api/documents/partages/envoyes/{employerId}
     * Documents envoyés par un employé.
     */
    @GetMapping("/partages/envoyes/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentPartageResponseDTO>>> getPartagesEnvoyes(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Partages envoyés récupérés",
                        documentService.getPartagesEnvoyes(employerId)));
    }

    // ── Marquer lu ───────────────────────────────────────────────────────────

    /**
     * PATCH /api/documents/{id}/lu
     * Marque un document partagé comme lu pour l'utilisateur courant.
     */
    @PatchMapping("/{id}/lu")
    public ResponseEntity<ApiResponse<Void>> marquerLu(@PathVariable Long id) {
        documentService.marquerLu(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Marqué comme lu", null));
    }

    // ── CRUD standard ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponseDTO>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents récupérés",
                        documentService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Document récupéré",
                        documentService.findById(id)));
    }

    @GetMapping("/employer/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getByEmployer(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents de l'employé récupérés",
                        documentService.findByEmployerId(employerId)));
    }

    @GetMapping("/projet/{projetId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getByProjet(
            @PathVariable Long projetId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents du projet récupérés",
                        documentService.findByProjetId(projetId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequestDTO dto) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Document modifié",
                        documentService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) throws IOException {
        documentService.delete(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Document supprimé", null));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long id) throws IOException {
        DocumentResponseDTO meta    = documentService.findById(id);
        byte[]              content = documentService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getNomFichier() + "\"")
                .contentType(MediaType.parseMediaType(
                        meta.getTypeMime() != null
                                ? meta.getTypeMime()
                                : "application/octet-stream"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }
}