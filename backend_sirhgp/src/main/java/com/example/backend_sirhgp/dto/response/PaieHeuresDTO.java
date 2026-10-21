// ─── PaieHeuresDTO.java ──────────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 5.1 — Résumé des heures pointées d'un employé pour un mois,
 * pont entre le pointage (4.1) et la fiche de paie.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaieHeuresDTO {

    private Long employeId;
    private String employeNom;
    private String nui;
    private String rib;
    private BigDecimal salaireBase;
    private Integer mois;
    private Integer annee;

    // ── Jours ──
    private int joursOuvres;      // lun-ven du mois
    private int joursEcoules;     // jours ouvrés passés (ou tout le mois si passé)
    private int joursPresents;    // pointages complets (arrivée + départ)
    private int joursManquants;   // écoules - présents

    // ── Heures ──
    private BigDecimal heuresNormales;  // dans la durée standard journalière
    private BigDecimal heuresSupJour;   // au-delà, en journée (06h-22h) → +20 %
    private BigDecimal heuresSupNuit;   // au-delà, la nuit (22h-06h) → +50 %
    private int retardsMinutes;         // cumul des retards au pointage d'arrivée

    // ── Tarifs & retenues suggérées ──
    private BigDecimal tarifHoraire;
    private BigDecimal retenuesAbsences; // joursManquants × tarif journalier
    private BigDecimal retenuesRetards;  // retardsMinutes × tarif horaire

    private List<String> avertissements;
    private List<PointageResponseDTO> pointages;
}
