package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refonte 5.9 (§6) — cible d'impersonation : liste minimale des utilisateurs d'un tenant
 * exposée UNIQUEMENT via /api/plateforme (le SUPER_ADMIN n'a pas accès /api/employers).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurCibleDTO {
    private Long id;
    private String name;
    private String email;
    private String roleName;
    private boolean actif;
}