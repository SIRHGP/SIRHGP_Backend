package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** Refonte 5.9 (§5) — journal des sauvegardes plateforme (exécution = script infrastructure). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponseDTO {
    private Long id;
    private LocalDateTime dateCreation;
    private String statut; // DEMANDE, EN_COURS, REUSSI, ECHEC
    private String message;
}