package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Permission regroupant un ensemble d'actions.
 * Une permission peut appartenir à plusieurs rôles et postes.
 *
 * CORRECTIONS :
 * - @Data remplacé par annotations explicites (déjà partiellement fait dans l'original).
 * - @CreationTimestamp et @UpdateTimestamp ajoutés — présents dans PermissionDTO
 *   mais absents du modèle original.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"actions", "roles"})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // CORRECTION : manquaient dans le modèle original
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToMany(mappedBy = "permissions")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "permission_actions",
            joinColumns = @JoinColumn(name = "permission_id"),
            inverseJoinColumns = @JoinColumn(name = "action_id")
    )
    @Builder.Default
    private Set<Action> actions = new HashSet<>();

    // ─── Méthodes utilitaires ─────────────────────────────────

    public void addAction(Action action) {
        this.actions.add(action);
    }

    public void addActions(Set<Action> newActions) {
        this.actions.addAll(newActions);
    }

    public void removeAction(Action action) {
        this.actions.remove(action);
    }

    public void clearActions() {
        this.actions.clear();
    }
}