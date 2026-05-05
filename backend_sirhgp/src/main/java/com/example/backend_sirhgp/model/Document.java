package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_destinataire_id", columnList = "destinataire_id"),
        @Index(name = "idx_expediteur_id", columnList = "expediteur_id"),
        @Index(name = "idx_document_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Métadonnées du fichier
    @Column(nullable = false)
    private String nom;

    private String description;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false)
    private String typeMime;

    @Column(nullable = false)
    private String cheminStockage;

    private Long taille;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument type;

    // Statut du document
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIF;

    // Visibilité
    @Builder.Default
    private boolean publique = false;

    @Builder.Default
    private Long nombreTelechargements = 0L;

    // Informations de partage (dispatch)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Employer expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id")
    private Employer destinataire;

    @Column(name = "est_lu")
    @Builder.Default
    private boolean estLu = false;

    @Column(name = "est_archive")
    @Builder.Default
    private boolean estArchive = false;

    private String message;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @CreationTimestamp
    @Column(name = "date_envoi", updatable = false)
    private LocalDateTime dateEnvoi;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    // Relations optionnelles
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id")
    private Employer employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    public enum TypeDocument {
        CONTRAT, RAPPORT, FACTURE, PRESENTATION, IMAGE, AUTRE
    }

    public enum DocumentStatus {
        ACTIF, TRANSFERE, SUPPRIME
    }

    public void incrementerTelechargements() {
        this.nombreTelechargements = (this.nombreTelechargements == null ? 0L : this.nombreTelechargements) + 1L;
    }

    public void marquerCommeLu() {
        this.estLu = true;
        this.dateLecture = LocalDateTime.now();
    }

    public boolean estEnvoye() {
        return this.destinataire != null;
    }

    public boolean appartientA(Long employeurId) {
        return (this.expediteur != null && this.expediteur.getId().equals(employeurId)) ||
                (this.destinataire != null && this.destinataire.getId().equals(employeurId));
    }
}