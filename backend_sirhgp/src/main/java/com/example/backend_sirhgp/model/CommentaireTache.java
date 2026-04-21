package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Commentaire posté sur une tâche par un employé.
 *
 * CORRECTIONS :
 * - @UpdateTimestamp ajouté — manquait alors que le DTO CommentaireResponseDTO
 *   avait un champ dateModification.
 * - estModifie ajouté — présent dans le DTO, absent du modèle original.
 * - @Data remplacé par @Getter + @Setter + @ToString/@EqualsAndHashCode explicites
 *   pour éviter les problèmes de lazy loading sur tache et auteur.
 * - @EqualsAndHashCode(callSuper = false) remplacé par onlyExplicitlyIncluded = true
 *   pour éviter d'inclure les entités liées dans le calcul d'égalité.
 */
@Entity
@Table(name = "commentaires_tache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"tache", "auteur"})
public class CommentaireTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    // CORRECTION : manquait dans le modèle original
    @UpdateTimestamp
    private LocalDateTime dateModification;

    // CORRECTION : champ présent dans le DTO mais absent du modèle
    @Builder.Default
    private Boolean estModifie = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tache_id", nullable = false)
    private Tache tache;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Employer auteur;

    /**
     * Méthode utilitaire : marque le commentaire comme modifié lors d'une mise à jour.
     * Appelée dans le service lors du update.
     */
    public void mettreAJourContenu(String nouveauContenu) {
        this.contenu = nouveauContenu;
        this.estModifie = true;
    }
}