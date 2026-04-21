package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Rôle attribué à un employé, définissant ses droits d'accès globaux.
 * Un rôle regroupe plusieurs permissions.
 *
 * CORRECTIONS :
 * - @Data remplacé par annotations explicites.
 * - @EqualsAndHashCode protégé avec onlyExplicitlyIncluded = true.
 *   Sans ça, Hibernate peut inclure la collection permissions dans le hashCode,
 *   ce qui déclenche un chargement lazy non désiré et provoque des bugs subtils
 *   avec les HashSet.
 * - @ToString exclut permissions pour éviter les boucles via Role → Permission → Role.
 * - @CreationTimestamp et @UpdateTimestamp ajoutés — présents dans RoleDTO
 *   mais absents du modèle original.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "permissions")
public class Role {

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

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    // ─── Méthodes utilitaires ─────────────────────────────────

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public void clearPermissions() {
        this.permissions.clear();
    }
}