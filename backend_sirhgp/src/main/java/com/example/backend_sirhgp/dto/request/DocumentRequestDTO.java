package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO métier pour les métadonnées du document.
 * CORRECTION : MultipartFile retiré — le fichier physique est géré séparément
 * dans le controller via @RequestPart("fichier") MultipartFile fichier.
 * Ce DTO ne contient que les métadonnées.
 */
@Data
public class DocumentRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le type de document est obligatoire")
    private Document.TypeDocument type;

    private Long employerId;   // document lié à un employé (nullable)
    private Long projetId;     // document lié à un projet (nullable)
    private boolean publique;
}