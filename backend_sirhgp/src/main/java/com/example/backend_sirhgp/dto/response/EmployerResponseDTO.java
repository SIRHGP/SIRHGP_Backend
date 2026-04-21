package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un employé.
 * CORRECTION : motDePasse supprimé — ne doit JAMAIS apparaître en réponse.
 * CORRECTION : statistiques détachées → chargées à la demande via endpoints dédiés.
 * Ici, on retourne les infos de base + contexte RH.
 */
@Data
public class EmployerResponseDTO {

    private Long id;
    private String name;
    private String email;
    private LocalDate dateNaiss;
    private String lieu;
    private String region;
    private String residence;
    private String numeroTelephone;
    private boolean actif;
    private LocalDate dateEmbauche;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Contexte organisationnel (noms seulement, pas les objets complets)
    private Long posteId;
    private String posteNom;
    private Long roleId;
    private String roleName;
    private Long departementId;
    private String departementNom;
}