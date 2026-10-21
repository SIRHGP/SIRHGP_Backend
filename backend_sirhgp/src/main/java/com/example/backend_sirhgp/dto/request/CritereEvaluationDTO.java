// ─── CritereEvaluationDTO.java ───────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CritereEvaluationDTO {

    @NotBlank(message = "Le libellé du critère est obligatoire")
    private String libelle;

    @DecimalMin(value = "0.0", message = "Note min 0")
    @DecimalMax(value = "20.0", message = "Note max 20")
    @NotNull(message = "La note du critère est obligatoire")
    private BigDecimal note;

    /**
     * Coefficient de pondération (sur 2 décimales max, max 99.99 pour correspondre
     * au type DECIMAL(3,2) de la colonne DB).
     */
    @DecimalMin(value = "0.0", message = "Coefficient doit être >= 0")
    @DecimalMax(value = "99.99", message = "Coefficient doit être <= 99.99 (limite DECIMAL(3,2))")
    private BigDecimal coefficient;
}