package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * DTO de mise à jour d'un employé.
 * Pas de mot de passe ici → passer par un endpoint dédié /change-password.
 * Pas d'email (changement d'email = process séparé avec vérification).
 */
@Data
public class EmployerUpdateDTO {

    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    private String name;

    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaiss;

    private String lieu;
    private String region;
    private String residence;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String numeroTelephone;

    private LocalDate dateEmbauche;
    @DecimalMin(value = "0.0", message = "Le salaire de base ne peut pas être négatif")
    private BigDecimal salaireBase;
    private String nui;
    private String rib;
    private String cvUrl;
    private Long posteId;
    private Long roleId;
    private Boolean actif;
    private Long entrepriseId;
    private Long sucursalId;
    private Long managerId;
}
