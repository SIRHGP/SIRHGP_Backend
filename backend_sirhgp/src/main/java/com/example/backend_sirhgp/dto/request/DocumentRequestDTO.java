package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Document.TypeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO pour l'upload et la création d'un document unifié.
 * Permet à la fois le stockage et le dispatch immédiat.
 */
@Data
public class DocumentRequestDTO {

    @NotBlank(message = "Le nom du document est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le type de document est obligatoire")
    private TypeDocument type;

    // --- Dispatch (envoi à un destinataire) ---
    // Si renseigné, le document est automatiquement envoyé à cet employé
    private Long destinataireId;

    // Message d'accompagnement lors de l'envoi
    private String message;

    // --- Associations optionnelles ---
    // Associer le document à un employé spécifique (ex: contrat d'un employé)
    private Long employerId;

    // Associer le document à un projet spécifique
    private Long projetId;

    // Visibilité publique (accessible sans authentification)
    private boolean publique;
}