package com.example.backend_sirhgp.dto.response;


import com.example.backend_sirhgp.model.Projet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO du tableau de bord projet.
 *
 * CORRECTIONS :
 * - Double → BigDecimal pour tous les montants financiers
 * - Projet.StatutProjet conservé dans les sous-DTOs car c'est un DTO de dashboard
 *   (lecture seule, couplage acceptable ici). À isoler si l'API devient publique.
 * - ProjetDetailDTO déplacé → ProjetDetailView consolidé (suppression doublon)
 * - DepartementProjetView : employes renommé en membres (cohérence)
 * - MembreDTO : Double → BigDecimal pour tauxCompletion retiré (c'est un ratio, Double ok)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjetDashboardDTO {

    // ─── Compteurs globaux ────────────────────────────────────
    private Integer totalProjets;
    private Integer projetsEnCours;
    private Integer projetsTermines;
    private Integer projetsEnRetard;
    private Integer projetsPlanifies;
    private Integer projetsSuspendus;
    private Integer projetsAnnules;

    // ─── Budget global ────────────────────────────────────────
    // CORRECTION : BigDecimal
    private BigDecimal budgetTotalAlloue;
    private BigDecimal budgetTotalConsomme;
    private BigDecimal budgetTotalRestant;
    private Double tauxConsommationGlobal;  // ratio 0-100, Double acceptable

    // ─── Tâches globales ──────────────────────────────────────
    private Integer totalTaches;
    private Integer tachesTerminees;
    private Integer tachesEnCours;
    private Integer tachesAFaire;
    private Double progressionMoyenne;

    // ─── Membres ──────────────────────────────────────────────
    private Integer totalMembres;

    // ─── Factures globales ────────────────────────────────────
    private Integer totalFactures;
    private BigDecimal montantTotalFactures;
    private BigDecimal montantTotalPaye;
    private BigDecimal montantTotalRestant;

    // ─── Listes de synthèse ───────────────────────────────────
    private List<ProjetSimpleDTO> projetsRecents;
    private List<ProjetSimpleDTO> projetsCritiques;
    private List<StatutStatDTO> repartitionParStatut;
    private List<ProgressionDTO> progressionProjets;
    private List<BudgetProjetDTO> topProjetsBudget;

    // =========================================================
    //  Sous-DTOs
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjetSimpleDTO {
        private Long id;
        private String nom;
        private String codeProjet;
        private Projet.StatutProjet statut;
        private String statutLibelle;
        private String statutCouleur;
        private Integer progression;
        private LocalDate dateDebut;
        private LocalDate dateFinPrevue;
        private LocalDate dateFinReelle;
        private Long chefProjetId;
        private String chefProjetNom;
        private BigDecimal budgetAlloue;   // CORRECTION
        private BigDecimal budgetConsomme; // CORRECTION
        private Boolean estEnRetard;
        private Integer joursRestants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatutStatDTO {
        private Projet.StatutProjet statut;
        private String libelle;
        private String couleur;
        private Integer count;
        private Double pourcentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressionDTO {
        private Long id;
        private String nom;
        private String codeProjet;
        private Integer progression;
        private Projet.StatutProjet statut;
        private String statutLibelle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetGlobalDTO {
        private BigDecimal totalAlloue;    // CORRECTION
        private BigDecimal totalConsomme;  // CORRECTION
        private BigDecimal totalRestant;   // CORRECTION
        private Double tauxConsommation;
        private List<BudgetProjetDTO> detailsParProjet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetProjetDTO {
        private Long id;
        private String nom;
        private String codeProjet;
        private Projet.StatutProjet statut;
        private BigDecimal alloue;     // CORRECTION
        private BigDecimal consomme;   // CORRECTION
        private BigDecimal restant;    // CORRECTION
        private Double pourcentage;
        private Boolean estDepasse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembreDTO {
        private Long id;
        private String nom;
        private String email;
        private String poste;
        private String departement;
        private String photo;
        private Integer tachesAssignees;
        private Integer tachesTerminees;
        private Double tauxCompletion;  // ratio 0-100, Double ok
    }

    /**
     * CORRECTION : ProjetDetailDTO et ProjetDetailView fusionnés en un seul DTO
     * pour éviter la duplication de champs identiques.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjetDetailView {
        private Long id;
        private String nom;
        private String codeProjet;
        private Projet.StatutProjet statut;
        private String statutLibelle;
        private LocalDate dateDebut;
        private LocalDate dateFinPrevue;
        private LocalDate dateFinReelle;
        private Integer progression;

        // Tâches
        private Integer totalTaches;
        private Integer tachesAFaire;
        private Integer tachesEnCours;
        private Integer tachesTerminees;
        private Map<String, Integer> tachesParPriorite;
        private Map<String, Integer> tachesParStatut;

        // Budget
        private BigDecimal budgetAlloue;    // CORRECTION
        private BigDecimal budgetConsomme;  // CORRECTION
        private BigDecimal budgetRestant;   // CORRECTION
        private Double budgetPourcentage;

        // Factures
        private Integer totalFactures;
        private BigDecimal montantTotalFactures;  // CORRECTION
        private BigDecimal montantPaye;           // CORRECTION
        private BigDecimal montantRestant;        // CORRECTION
        private Map<String, Integer> facturesParStatut;

        // Équipe
        private Integer nombreMembres;
        private List<MembreDTO> membresEquipe;
        private Integer nombreDepartements;
        private List<String> nomsDepartements;

        // Chef de projet
        private Long chefProjetId;
        private String chefProjetNom;
        private String chefProjetEmail;

        // Départements avec leurs données
        private List<DepartementProjetView> departements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartementProjetView {
        private Long id;
        private String nom;
        // CORRECTION : "employes" renommé "membres" pour cohérence avec le reste du projet
        private List<MembreDTO> membres;
        private List<TacheProjetView> taches;
        private List<FactureProjetView> factures;
        private List<EvenementProjetView> evenements;
    }

    // ─── Vues allégées pour les listes imbriquées ─────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TacheProjetView {
        private Long id;
        private String titre;
        private String statut;
        private String priorite;
        private LocalDate dateEcheance;
        private String assigneeNom;
        private Boolean estEnRetard;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FactureProjetView {
        private Long id;
        private String numeroFacture;
        private String titre;
        private BigDecimal montantTTC;  // CORRECTION
        private String statut;
        private LocalDate dateEcheance;
        private Boolean estPayee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvenementProjetView {
        private Long id;
        private String titre;
        private String type;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private Long nombreParticipants;
    }
}
