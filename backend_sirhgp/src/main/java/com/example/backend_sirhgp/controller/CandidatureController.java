// ─── CandidatureController.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.request.CandidatureDecisionRequest;
import com.example.backend_sirhgp.dto.request.CandidatureRequestDTO;
import com.example.backend_sirhgp.dto.request.EmbaucheRequestDTO;
import com.example.backend_sirhgp.dto.response.CandidatureResponseDTO;
import com.example.backend_sirhgp.model.Candidature;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.CandidatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureService candidatureService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> create(
            @Valid @RequestBody CandidatureRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Candidature enregistrée avec succès",
                        candidatureService.create(dto)));
    }

    // ─── Refonte 5.7 : création avec CV téléversé (multipart) ─────────────
    // Une seule requête : partie « candidature » (JSON) + partie « cv » (fichier).
    // Le scope départemental (LEAD) est vérifié dans le service via l'offre.
    @PostMapping(value = "/avec-cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> createAvecCv(
            @RequestPart("candidature") @Valid CandidatureRequestDTO dto,
            @RequestPart("cv") MultipartFile cv) throws java.io.IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Candidature enregistrée avec succès (CV joint)",
                        candidatureService.createAvecCv(dto, cv)));
    }

    // ─── Refonte 5.7 : ajout/remplacement du CV sur une candidature existante
    @PostMapping(value = "/{id}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> uploadCv(
            @PathVariable Long id,
            @RequestPart("cv") MultipartFile cv) throws java.io.IOException {
        return ResponseEntity.ok(new ApiResponse<>(true, "CV ajouté avec succès",
                candidatureService.uploadCv(id, cv)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidatureResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Candidatures récupérées avec succès",
                candidatureService.findAll()));
    }

    @GetMapping("/offre/{offreId}")
    public ResponseEntity<ApiResponse<List<CandidatureResponseDTO>>> getByOffre(@PathVariable Long offreId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Candidatures de l'offre récupérées",
                candidatureService.findByOffreId(offreId)));
    }

    /** Avancement dans le pipeline (NOUVELLE → ENTRETIEN → EVALUATION → RECRUTE/REFUSEE). */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> updateStatut(
            @PathVariable Long id,
            @RequestParam Candidature.StatutCandidature statut,
            @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Statut de la candidature mis à jour",
                candidatureService.updateStatut(id, statut, commentaire)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        candidatureService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Candidature supprimée avec succès", null));
    }

    /** Pré-évaluation du dossier candidat (4.3) : verdict + note + avis. */
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> evaluer(
            @PathVariable Long id, @Valid @RequestBody CandidatureDecisionRequest dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Pré-évaluation enregistrée", candidatureService.evaluer(id, dto)));
    }

    /** Embauche du candidat : crée l'employé + bascule la candidature en RECRUTE/ENGAGE (4.3). Refonte 5.7 : LEAD/MANAGER inclus (scope vérifié dans le service). */
    @PostMapping("/{id}/embaucher")
    @PreAuthorize("hasAnyRole('LEAD','MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CandidatureResponseDTO>> embaucher(
            @PathVariable Long id, @Valid @RequestBody EmbaucheRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Candidat embauché avec succès", candidatureService.embaucher(id, dto)));
    }
}