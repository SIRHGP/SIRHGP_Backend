package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Document stocké dans le système (contrat, CV, bulletin de paie, etc.).
 *
 * CORRECTIONS :
 * - @Data remplacé par annotations explicites.
 * - @EqualsAndHashCode(callSuper = false) remplacé par onlyExplicitlyIncluded = true.
 * - @ToString exclut les relations lazy pour éviter les LazyInitializationException.
 * - nombreTelechargements ajouté — présent dans DocumentResponseDTO mais absent du modèle.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"employer", "uploader", "projet"})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String nomFichier;

    private String description;

    @Column(nullable = false)
    private String typeMime;

    @Column(nullable = false)
    private String cheminStockage;

    private Long taille;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    @Builder.Default
    private boolean publique = false;

    // CORRECTION : manquait dans le modèle original alors que le DTO l'avait
    @Builder.Default
    private Long nombreTelechargements = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private Employer employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private Employer uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    public enum TypeDocument {
       CONTRAT,RAPPORT,FACTURE,PRESENTATION,IMAGE,AUTRE
    }

    /**
     * Incrémente le compteur de téléchargements.
     * Appelé dans le service lors du download.
     */
    public void incrementerTelechargements() {
        this.nombreTelechargements = (this.nombreTelechargements == null ? 0L : this.nombreTelechargements) + 1L;
    }
}