package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.DocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.DocumentTransferRequestDTO;
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

    // ─── Upload avec dispatch optionnel ───────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> upload(
            @RequestPart("document") @Valid DocumentRequestDTO dto,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(201).body(
                new ApiResponse<>(true, "Document uploadé avec succès",
                        documentService.uploadWithDispatch(dto, fichier)));
    }

    // ─── Transfert d'un document existant ─────────────────────────────────────

    @PostMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> transfer(
            @PathVariable Long id,
            @Valid @RequestBody DocumentTransferRequestDTO dto) {
        return ResponseEntity.status(201).body(
                new ApiResponse<>(true, "Document transféré avec succès",
                        documentService.transferDocument(id, dto)));
    }

    // ─── Documents reçus ──────────────────────────────────────────────────────

    @GetMapping("/received/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getReceivedDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents reçus récupérés",
                        documentService.getReceivedDocuments(employerId)));
    }

    // ─── Documents non lus ────────────────────────────────────────────────────

    @GetMapping("/unread/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getUnreadDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents non lus récupérés",
                        documentService.getUnreadDocuments(employerId)));
    }


// ─── Récupérer les métadonnées ──────────────────────────────────────────────

    @GetMapping("/{id}/metadata")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Métadonnées récupérées",
                        documentService.getDocumentMetadata(id)));
    }

    // ─── Documents envoyés ────────────────────────────────────────────────────

    @GetMapping("/sent/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getSentDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents envoyés récupérés",
                        documentService.getSentDocuments(employerId)));
    }

    // ─── Mes documents (authentifié) ──────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<DocumentResponseDTO>>> getMyDocuments(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mes documents récupérés",
                        documentService.getMyDocuments(pageable)));
    }

    // ─── Marquer comme lu ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        documentService.markAsRead(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document marqué comme lu", null));
    }

    // ─── Archiver ─────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long id) {
        documentService.archiveDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document archivé", null));
    }

    // ─── Téléchargement ───────────────────────────────────────────────────────

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) throws IOException {
        DocumentResponseDTO metadata = documentService.getDocumentMetadata(id);
        byte[] content = documentService.downloadDocument(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getNomFichier() + "\"")
                .contentType(MediaType.parseMediaType(
                        metadata.getTypeMime() != null ? metadata.getTypeMime() : "application/octet-stream"))
                .body(new ByteArrayResource(content));
    }

    // ─── Documents publics ────────────────────────────────────────────────────

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getPublicDocuments() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents publics récupérés",
                        documentService.getPublicDocuments()));
    }

    // ─── Suppression ──────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) throws IOException {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document supprimé", null));
    }
}