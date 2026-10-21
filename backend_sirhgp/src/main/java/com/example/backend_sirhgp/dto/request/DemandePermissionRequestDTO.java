package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.DemandePermission.TypePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Refonte 5.6 — Création d'une demande de permission (Timesheet).
 * Le demandeur et le valideur ne sont PAS fournis : ils sont résolus
 * côté service (utilisateur courant + chaîne de validation par rôle).
 */
@Data
public class DemandePermissionRequestDTO {

    @NotNull(message = "Le type de permission est obligatoire")
    private TypePermission type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    // SORTIE uniquement (demi-journée / quelques heures)
    private LocalTime heureDebut;
    private LocalTime heureFin;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 1000, message = "Le motif ne doit pas dépasser 1000 caractères")
    private String motif;

    /** Justificatif : id d'un Document déjà téléversé via /api/documents/upload (optionnel). */
    private Long justificatifId;
}
