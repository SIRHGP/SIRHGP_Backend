package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Projet regroupant tâches, membres, départements, factures, événements et documents.
 *
 * CORRECTIONS :
 * - Double → BigDecimal pour budgetAlloue, budgetConsomme, budgetRestant.
 * - StatutProjet.EN_RETARD ajouté — utilisé dans les DTOs dashboard mais absent du modèle.
 * - updateBudget() utilise BigDecimal.subtract().
 * - @Data remplacé par annotations explicites.
 * - @EqualsAndHashCode(callSuper = false, exclude = {...}) remplacé par
 *   onlyExplicitlyIncluded = true — plus sûr et plus explicite.
 */
@Entity
@Table(name = "projets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"chefProjet", "membres", "taches", "departements",
        "factures", "evenements", "documents"})
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true)
    @EqualsAndHashCode.Include
    private String codeProjet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutProjet statut = StatutProjet.PLANIFIE;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFinPrevue;
    private LocalDate dateFinReelle;

    // CORRECTION : BigDecimal pour tous les montants budgétaires
    @Column(precision = 15, scale = 2)
    private BigDecimal budgetAlloue;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetConsomme = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetRestant = BigDecimal.ZERO;

    @Builder.Default
    private Integer progression = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_projet_id")
    private Employer chefProjet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToMany
    @JoinTable(
            name = "projet_membres",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "employer_id")
    )
    @Builder.Default
    private Set<Employer> membres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "projet_departements",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "departement_id")
    )
    @Builder.Default
    private Set<Departement> departements = new HashSet<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Tache> taches = new HashSet<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Facture> factures = new HashSet<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Evenement> evenements = new HashSet<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Document> documents = new HashSet<>();

    // ─── Enum ─────────────────────────────────────────────────────────────────

    public enum StatutProjet {
        PLANIFIE,
        EN_COURS,
        SUSPENDU,
        TERMINE,
        ANNULE,
        // CORRECTION : EN_RETARD manquait dans l'original alors qu'il était
        // utilisé dans ProjetDashboardDTO et les DTOs de réponse.
        EN_RETARD
    }

    // ─── Méthodes utilitaires ─────────────────────────────────────────────────

    public void addMembre(Employer membre) {
        this.membres.add(membre);
    }

    public void removeMembre(Employer membre) {
        this.membres.remove(membre);
    }

    public void addDepartement(Departement departement) {
        this.departements.add(departement);
    }

    public void removeDepartement(Departement departement) {
        this.departements.remove(departement);
    }

    public void addFacture(Facture facture) {
        this.factures.add(facture);
        facture.setProjet(this);
    }

    public void removeFacture(Facture facture) {
        this.factures.remove(facture);
        facture.setProjet(null);
    }

    public void addEvenement(Evenement evenement) {
        this.evenements.add(evenement);
        evenement.setProjet(this);
    }

    public void removeEvenement(Evenement evenement) {
        this.evenements.remove(evenement);
        evenement.setProjet(null);
    }

    public void addDocument(Document document) {
        this.documents.add(document);
        document.setProjet(this);
    }

    public void removeDocument(Document document) {
        this.documents.remove(document);
        document.setProjet(null);
    }

    /**
     * CORRECTION : calcul avec BigDecimal.subtract().
     * Aussi : mise à jour automatique du statut EN_RETARD si la date est dépassée.
     */
    public void updateBudget() {
        if (this.budgetAlloue != null && this.budgetConsomme != null) {
            this.budgetRestant = this.budgetAlloue
                    .subtract(this.budgetConsomme)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Vérifie si le projet est en retard et met à jour le statut si nécessaire.
     * À appeler dans le service lors des mises à jour.
     */
    public void verifierEtMettreAJourRetard() {
        if (this.statut == StatutProjet.EN_COURS
                && this.dateFinPrevue != null
                && this.dateFinPrevue.isBefore(LocalDate.now())
                && this.progression < 100) {
            this.statut = StatutProjet.EN_RETARD;
        }
    }
}
