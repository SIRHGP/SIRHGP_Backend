package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Département de l'organisation.
 * Un département regroupe des postes et peut être associé à plusieurs projets.
 * La relation avec Projet est gérée côté Projet (ManyToMany owner = Projet).
 *
 * CORRECTIONS :
 * - @CreationTimestamp et @UpdateTimestamp ajoutés — présents dans DepartementDTO
 *   mais absents du modèle original.
 * - @Data remplacé par annotations explicites pour éviter toString récursif.
 * - @EqualsAndHashCode protégé avec onlyExplicitlyIncluded = true.
 * - @ToString exclut la collection postes pour éviter le lazy loading involontaire.
 */
@Entity
@Table(name = "departements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "postes")
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private String name;

    // CORRECTION : absents dans le modèle original, présents dans le DTO
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @OneToMany(mappedBy = "departement")
    @Builder.Default
    private Set<Poste> postes = new HashSet<>();

    // Note : la relation avec Projet n'est pas déclarée ici car
    // Projet est le propriétaire de la relation ManyToMany (JoinTable côté Projet).
    // Pour lister les projets d'un département, utiliser une requête JPQL dédiée
    // dans le repository plutôt qu'une relation bidirectionnelle qui complexifie
    // inutilement le graphe d'objets.
}