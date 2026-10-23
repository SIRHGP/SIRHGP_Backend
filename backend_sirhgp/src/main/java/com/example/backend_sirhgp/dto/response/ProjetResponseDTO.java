package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Projet;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de réponse pour un projet.
 * CORRECTION : Double → BigDecimal pour tous les montants financiers.
 * CORRECTION : pas de triple redondance (id + nom + objet complet).
 *   → On retourne l'objet résumé EmployerSummaryDTO pour le chef de projet
 *     et les membres, pas l'EmployerResponseDTO complet.
 */
@Data
public class ProjetResponseDTO {

    private Long id;
    private String nom;
    private String description;
    private String codeProjet;
    private Long entrepriseId;
    private Long sucursalId;
    private Projet.StatutProjet statut;
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;
    private LocalDate dateFinReelle;

    // CORRECTION : BigDecimal
    private BigDecimal budgetAlloue;
    private BigDecimal budgetConsomme;
    private BigDecimal budgetRestant;
    private Integer progression;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Chef de projet : résumé seulement
    private EmployerSummaryDTO chefProjet;

    // Membres : résumés
    private Set<EmployerSummaryDTO> membres;

    // Départements : résumés
    private Set<DepartementSummaryDTO> departements;

    // Compteurs (calculés) — pas les listes complètes pour éviter N+1
    private Long nombreTaches;
    private Long nombreTachesTerminees;
    private Long nombreTachesEnCours;
    private Long nombreTachesEnRetard;
    private Long nombreFactures;
    private Long nombreMembres;
    private Long nombreDepartements;
    private Long nombreEvenements;
    private Long nombreDocuments;

    // Indicateurs financiers
    private BigDecimal montantTotalFactures;
    private BigDecimal montantPayeFactures;
    private BigDecimal montantRestantFactures;
    private Long nombreFacturesPayees;
    private Long nombreFacturesEnAttente;

    // Indicateurs temporels
    private Boolean estEnRetard;
    private Long joursRestants;
    private BigDecimal depassementBudget;

    // ─── Objets résumés imbriqués ─────────────────────────────

    @Data
    public static class EmployerSummaryDTO {
        private Long id;
        private String name;
        private String email;
        private String posteName;
        private String departementNom;
    }

    @Data
    public static class DepartementSummaryDTO {
        private Long id;
        private String name;
        private Long nombreEmployes;
    }
}
