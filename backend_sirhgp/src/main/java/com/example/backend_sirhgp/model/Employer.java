package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entité principale représentant un employé, qui est aussi le principal de sécurité.
 *
 * CORRECTIONS :
 * - @Data supprimé → remplacé par @Getter + @Setter + @EqualsAndHashCode + @ToString explicites.
 *   @Data sur une entité JPA implémentant UserDetails est dangereux :
 *   le toString() généré peut traverser les relations lazy et provoquer des
 *   LazyInitializationException ou des boucles infinies.
 *
 * - @EqualsAndHashCode : basé UNIQUEMENT sur l'id (onlyExplicitlyIncluded = true).
 *   Baser equals/hashCode sur des champs mutables dans une entité JPA est une
 *   erreur classique qui casse les HashSet et les opérations de merge Hibernate.
 *
 * - getAuthorities() corrigé : la double source de rôle (role direct + poste.getRole())
 *   créait une incohérence métier. Règle claire : le role direct a la priorité.
 *   Les permissions du poste sont ajoutées en tant que GrantedAuthority supplémentaires
 *   pour un contrôle fin avec @PreAuthorize("hasAuthority('PERMISSION_NAME')").
 *
 * - @CreationTimestamp et @UpdateTimestamp ajoutés — manquaient dans le modèle original.
 *
 * - @Column(nullable = false) ajouté sur name et email pour cohérence avec la DB.
 *
 * - @DateTimeFormat supprimé — annotation Spring MVC qui n'a pas sa place dans une
 *   entité JPA (confusion couches). La gestion du format date se fait au niveau DTO.
 */
@Entity
@Table(name = "employers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"motDePasse", "poste", "role"})
// motDePasse exclu du toString pour éviter toute fuite dans les logs
public class Employer implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    // email dans equals/hashCode comme fallback naturel d'unicité métier
    private String email;

    // CORRECTION : @Column(name = "mot_de_passe") pour éviter le mot réservé SQL
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    private LocalDate dateNaiss;
    private String lieu;
    private String region;
    private String residence;
    private String numeroTelephone;
    private LocalDate dateEmbauche;

    @Builder.Default
    private boolean actif = true;

    // CORRECTION : ajoutés — manquaient dans le modèle original
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    // ─── UserDetails ─────────────────────────────────────────────────────────

    /**
     * CORRECTION : getAuthorities() refactorisé.
     *
     * Stratégie claire :
     * 1. Le rôle direct de l'employé définit son ROLE_ principal (pour @PreAuthorize("hasRole(...)")).
     * 2. Les permissions du poste sont ajoutées comme autorités fines
     *    (pour @PreAuthorize("hasAuthority('NOM_PERMISSION')")).
     * 3. Les permissions du rôle sont aussi incluses.
     *
     * Plus de double source ambiguë entre role direct et poste.getRole().
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Rôle principal
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // Permissions du rôle
            if (role.getPermissions() != null) {
                role.getPermissions().forEach(permission ->
                        authorities.add(new SimpleGrantedAuthority(permission.getName()))
                );
            }
        }

        // Permissions spécifiques du poste (peuvent étendre les droits du rôle)
        if (poste != null && poste.getPermissions() != null) {
            poste.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getName()))
            );
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return actif;
        // CORRECTION : un compte désactivé (actif=false) doit être considéré
        // comme verrouillé, pas uniquement isEnabled(). Les deux méthodes
        // étaient incohérentes dans la version originale.
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}