// ─── FeuilleTempsRequestDTO.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.FeuilleTemps;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeuilleTempsRequestDTO {

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "Les heures sont obligatoires")
    @DecimalMin(value = "0.01", message = "Heures supérieures à 0")
    @DecimalMax(value = "24.00", message = "Heures max 24 par jour")
    private BigDecimal heures;

    private String description;

    private Long projetId;
    private Long tacheId;

    private FeuilleTemps.StatutFeuille statut;
}