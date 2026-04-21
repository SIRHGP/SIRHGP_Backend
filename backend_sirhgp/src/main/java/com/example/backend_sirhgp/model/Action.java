package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Représente une action atomique du système (ex: CREATE_USER, DELETE_DOCUMENT).
 * Une action est associée à plusieurs permissions.
 *
 * CORRECTIONS :
 * - Aucune correction majeure sur ce modèle, il était déjà bien structuré.
 * - @EqualsAndHashCode(onlyExplicitlyIncluded = true) conservé → bonne pratique
 *   pour éviter les boucles infinies avec les collections JPA.
 * - @ToString ajouté en excluant la collection "permissions" pour éviter
 *   les appels récursifs en cas de lazy loading ou de boucle bidirectionnelle.
 */
@Entity
@Table(name = "actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "permissions")
// CORRECTION : @Data remplacé par @Getter + @Setter + annotations explicites
// @Data génère un toString() qui peut traverser les collections JPA et
// provoquer des LazyInitializationException ou des boucles infinies.
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToMany(mappedBy = "actions")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}