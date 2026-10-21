// ─── FeuilleTempsResponseDTO.java ────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.FeuilleTemps;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FeuilleTempsResponseDTO {

    private Long id;
    private LocalDate date;
    private BigDecimal heures;
    private String description;
    private FeuilleTemps.StatutFeuille statut;

    private Long employeId;
    private String employeName;

    private Long projetId;
    private String projetNom;

    private Long tacheId;
    private String tacheTitre;
}