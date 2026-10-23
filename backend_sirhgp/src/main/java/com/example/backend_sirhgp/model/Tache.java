package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tâche assignée à un employé dans le cadre d'un projet.
 *
 * CORRECTIONS :
 * - @EqualsAndHashCode(callSuper = false) → onlyExplicitlyIncluded = true.
 *   Sans ça, tous les champs (dont les collections et les relations) sont
 *   inclus dans equals/hashCode, ce qui peut déclencher des lazy loading
 *   non voulus et des StackOverflowError.
 * - @Data remplacé par annotations explicites.
 * - @ToString exclut les relations pour éviter les boucles circulaires
 *   (Tache → Projet → Set<Tache> → Tache...).
 */
@Entity
@Table(name = "taches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"createur", "assignee", "projet", "commentaires"})
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutTache statut = StatutTache.A_FAIRE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priorite priorite = Priorite.MOYENNE;

    private LocalDate dateEcheance;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id")
    private Employer createur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Employer assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibilite visibilite = Visibilite.SUCURSALE;

    @OneToMany(mappedBy = "tache", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CommentaireTache> commentaires = new HashSet<>();

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum StatutTache {
        A_FAIRE,
        EN_COURS,
        EN_REVUE,
        TERMINEE,
        BLOQUEE
    }

    public enum Priorite {
        BASSE,
        MOYENNE,
        HAUTE,
        URGENTE
    }

    public enum Visibilite { PRIVEE, SUCURSALE, ENTREPRISE, @Deprecated PUBLIQUE }

    // ─── Méthodes utilitaires ─────────────────────────────────────────────────

    public void addCommentaire(CommentaireTache commentaire) {
        this.commentaires.add(commentaire);
        commentaire.setTache(this);
    }

    public void removeCommentaire(CommentaireTache commentaire) {
        this.commentaires.remove(commentaire);
        commentaire.setTache(null);
    }

    /**
     * Indique si la tâche est en retard (date dépassée et non terminée).
     */
    public boolean isEnRetard() {
        return this.dateEcheance != null
                && this.dateEcheance.isBefore(LocalDate.now())
                && this.statut != StatutTache.TERMINEE;
    }
}
