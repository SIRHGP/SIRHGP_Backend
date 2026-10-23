package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * DTO de création d'un employé.
 * Contient UNIQUEMENT les champs modifiables par le client,
 * plus les validations métier.
 * Le mot de passe est ici car c'est une entrée de création — jamais dans la réponse.
 */
@Data
public class EmployerCreateDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit faire entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String motDePasse;

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

    @NotNull(message = "Le poste est obligatoire")
    private Long posteId;

    @NotNull(message = "Le rôle est obligatoire")
    private Long roleId;
    private Long entrepriseId;
    private Long sucursalId;
    private Long managerId;
}
