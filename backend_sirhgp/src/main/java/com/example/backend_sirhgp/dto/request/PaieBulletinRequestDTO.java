// ─── PaieBulletinRequestDTO.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/** 5.1 — Génération d'un bulletin unitaire à partir des heures pointées. */
@Data
public class PaieBulletinRequestDTO {
    private Long employeId;
    private Integer mois;      // 1..12
    private Integer annee;
    private BigDecimal primes;          // optionnel
    private BigDecimal indemnites;      // optionnel
    private BigDecimal heuresSupJour;   // sinon recalculé depuis les pointages
    private BigDecimal heuresSupNuit;
    private BigDecimal retenuesDiverses; // absences + retards (pré-rempli par le front)
}
