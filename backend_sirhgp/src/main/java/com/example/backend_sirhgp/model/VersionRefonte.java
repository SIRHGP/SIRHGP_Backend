package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Refonte 5.9 (§5) — suivi des refontes / versions 5.x (seed au démarrage,
 * historique issu de doc/update.md).
 */
@Entity
@Table(name = "version_refontes", indexes = @Index(name = "idx_version_numero", columnList = "numero"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VersionRefonte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Numéro de refonte, ex. "5.9". */
    @Column(unique = true, nullable = false, length = 20)
    private String numero;

    private LocalDate dateSortie;

    @Column(columnDefinition = "TEXT")
    private String resume;
}