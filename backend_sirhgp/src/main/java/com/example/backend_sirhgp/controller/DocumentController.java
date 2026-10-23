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

    // ─── Upload simplifié (alias) ─────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> uploadDocument(
            @RequestPart("document") @Valid DocumentRequestDTO dto,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return upload(dto, fichier);
    }

    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> uploadMyCv(
            @RequestParam(defaultValue = "Mon CV") String nom,
            @RequestPart("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.status(201).body(new ApiResponse<>(true, "CV téléversé avec succès",
                documentService.uploadMyCv(nom, fichier)));
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

    // ─── Mise à jour d'un document ────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document mis à jour avec succès",
                documentService.updateDocument(id, dto)));
    }

    // ─── Liste tous les documents (avec pagination) ───────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentResponseDTO>>> getAllDocuments(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents récupérés avec succès",
                        documentService.getAllDocuments(pageable)));
    }

    // ─── Mes documents (utilisateur connecté) ─────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<DocumentResponseDTO>>> getMyDocuments(Pageable pageable) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mes documents récupérés",
                        documentService.getMyDocuments(pageable)));
    }

    // ─── Récupérer les métadonnées ────────────────────────────────────────────
    @GetMapping("/{id}/metadata")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> getMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Métadonnées récupérées",
                        documentService.getDocumentMetadata(id)));
    }

    // ─── Téléchargement (CHEMIN CORRECT) ──────────────────────────────────────
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

    // ─── Documents reçus ──────────────────────────────────────────────────────
    @GetMapping("/received/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getReceivedDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents reçus récupérés",
                        documentService.getReceivedDocuments(employerId)));
    }

    // ─── Documents envoyés ────────────────────────────────────────────────────
    @GetMapping("/sent/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getSentDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents envoyés récupérés",
                        documentService.getSentDocuments(employerId)));
    }

    // ─── Documents non lus ────────────────────────────────────────────────────
    @GetMapping("/unread/{employerId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getUnreadDocuments(
            @PathVariable Long employerId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents non lus récupérés",
                        documentService.getUnreadDocuments(employerId)));
    }

    // ─── Documents publics ────────────────────────────────────────────────────
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<DocumentResponseDTO>>> getPublicDocuments() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Documents publics récupérés",
                        documentService.getPublicDocuments()));
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

    // ─── Suppression ──────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) throws IOException {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document supprimé", null));
    }
}
