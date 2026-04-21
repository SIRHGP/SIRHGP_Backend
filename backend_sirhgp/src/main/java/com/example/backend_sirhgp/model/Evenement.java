package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Événement du calendrier (réunion, formation, congé, etc.).
 *
 * CORRECTIONS :
 * - @Data remplacé par annotations explicites.
 * - @EqualsAndHashCode(callSuper = false) → onlyExplicitlyIncluded = true.
 * - @ToString exclut createur, participants et projet.
 */
@Entity
@Table(name = "evenements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"createur", "participants", "projet"})
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalTime heureDebut;
    private LocalDate dateFin;
    private LocalTime heureFin;
    private String lieu;
    private String couleur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TypeEvenement type = TypeEvenement.REUNION;

    private Integer minutesRappel;

    @Builder.Default
    private boolean rappelActif = false;

    @Builder.Default
    private boolean jourEntier = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id")
    private Employer createur;

    @ManyToMany
    @JoinTable(
            name = "evenement_participants",
            joinColumns = @JoinColumn(name = "evenement_id"),
            inverseJoinColumns = @JoinColumn(name = "employer_id")
    )
    @Builder.Default
    private Set<Employer> participants = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    public enum TypeEvenement {
        REUNION, FORMATION, CONGE, MISSION, ENTREVUE, DEADLINE, AUTRE
    }

    // ─── Méthodes utilitaires ─────────────────────────────────

    public void addParticipant(Employer employer) {
        this.participants.add(employer);
    }

    public void removeParticipant(Employer employer) {
        this.participants.remove(employer);
    }
}