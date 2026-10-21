// ─── SuperAdminStatsDTO.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Statistiques transverses du Super Admin (toutes entreprises confondues).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminStatsDTO {

    private long totalEntreprises;
    private long entreprisesActives;
    private long entreprisesSuspendues;
    private long totalEmployes;
    private long totalProjets;
    private long totalTaches;
    private long totalFactures;
    private BigDecimal montantTotalFactures;

    private List<EntrepriseStatsDTO> parEntreprise;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntrepriseStatsDTO {
        private Long entrepriseId;
        private String nom;
        private String statut;
        private long nbEmployes;
        private long nbProjets;
        private long nbTaches;
        private long nbFactures;
        private BigDecimal montantFactures;
    }
}