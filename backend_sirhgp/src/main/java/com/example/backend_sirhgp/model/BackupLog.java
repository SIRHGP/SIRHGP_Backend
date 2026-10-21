package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Refonte 5.9 (§5) — journal des sauvegardes plateforme.
 * POST déclenche une demande (statut DEMANDE) ; le script infrastructure
 * reporte le résultat via PATCH /api/plateforme/backups/{id}/statut.
 */
@Entity
@Table(name = "backup_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BackupLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutBackup statut;

    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    public enum StatutBackup { DEMANDE, EN_COURS, REUSSI, ECHEC }
}