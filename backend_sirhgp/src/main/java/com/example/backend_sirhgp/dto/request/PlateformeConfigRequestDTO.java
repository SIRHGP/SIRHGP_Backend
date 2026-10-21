package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Refonte 5.9 (§5) — PUT /api/plateforme/config : upsert d'un paramètre global (clé/valeur). */
@Data
public class PlateformeConfigRequestDTO {
    @NotBlank(message = "La clé est obligatoire")
    private String cle;
    private String valeur;
    private String description;
}