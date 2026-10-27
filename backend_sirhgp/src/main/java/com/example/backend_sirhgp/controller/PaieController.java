// ─── PaieController.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.dto.response.BulletinPaieResponseDTO;
import com.example.backend_sirhgp.dto.response.PaieHeuresDTO;
import com.example.backend_sirhgp.dto.request.PaieBulletinRequestDTO;
import com.example.backend_sirhgp.model.BulletinPaie;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.PaiePdfService;
import com.example.backend_sirhgp.service.PaieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paie")
@RequiredArgsConstructor
public class PaieController {

    private final PaieService paieService;
    private final PaiePdfService paiePdfService;

    /** 5.1 — Résumé des heures pointées d'un employé pour un mois (fiche employé → paie). */
    @GetMapping("/employes/{id}/heures/{mois}/{annee}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD', 'STAFF', 'STAGIAIRE', 'SECRETAIRE')")
    public ResponseEntity<ApiResponse<PaieHeuresDTO>> heuresEmploye(
            @PathVariable Long id, @PathVariable Integer mois, @PathVariable Integer annee) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Heures du mois récupérées",
                paieService.getHeuresEmploye(id, mois, annee)));
    }

    /** 5.1 — Synthèse des heures de tous les employés actifs pour un mois (vue Timesheet → équipe). */
    @GetMapping("/heures")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
    public ResponseEntity<ApiResponse<List<PaieHeuresDTO>>> heuresEntreprise(
            @RequestParam Integer mois, @RequestParam Integer annee) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Heures du mois (équipe)",
                paieService.getHeuresEntreprise(mois, annee)));
    }

    /** 5.1 — Génération d'un bulletin unitaire (depuis la fiche employé). */
    @PostMapping("/bulletins/generer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDTO>> genererUnitaire(
            @RequestBody PaieBulletinRequestDTO req) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulletin généré",
                paieService.genererBulletinUnitaire(req)));
    }

    /** Modification d'un bulletin (recalcul complet des montants dérivés). */
    @PutMapping("/bulletins/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BulletinPaieResponseDTO>> modifierBulletin(
            @PathVariable Long id, @RequestBody PaieBulletinRequestDTO req) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulletin modifié",
                paieService.updateBulletin(id, req)));
    }

    /** Suppression d'un bulletin. */
    @DeleteMapping("/bulletins/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> supprimerBulletin(@PathVariable Long id) {
        paieService.deleteBulletin(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulletin supprimé", null));
    }

    /** Génère les bulletins de l'entreprise courante pour une période. */
    @PostMapping("/generer/{mois}/{annee}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BulletinPaieResponseDTO>>> generer(
            @PathVariable Integer mois, @PathVariable Integer annee) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulletins générés",
                paieService.genererBulletinsPourEntreprise(mois, annee)));
    }

    /** Consultation des bulletins (scopée par entreprise / succursale ; STAFF/STAGIAIRE = ses propres bulletins). */
    @GetMapping("/bulletins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD', 'STAFF', 'STAGIAIRE', 'SECRETAIRE')")
    public ResponseEntity<ApiResponse<Page<BulletinPaieResponseDTO>>> consulter(
            Pageable pageable,
            @RequestParam(required = false) Long employeId,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) Integer annee) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bulletins récupérés",
                paieService.consulter(pageable, employeId, mois, annee)));
    }

    /** Export PDF d'un bulletin (accès borné par PaieService : self pour STAFF/STAGIAIRE). */
    @GetMapping("/bulletins/{id}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD', 'STAFF', 'STAGIAIRE', 'SECRETAIRE')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        BulletinPaie b = paieService.getEntityForPdf(id);
        byte[] content;
        try {
            content = paiePdfService.generate(b);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Erreur génération PDF : " + e.getMessage(), e);
        }
        String filename = "bulletin-" + b.getId() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
