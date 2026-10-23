package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Projet;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class ProjetRequestDTO {

    @NotBlank(message = "Le nom du projet est obligatoire")
    @Size(max = 200)
    private String nom;

    private String description;

    @NotBlank(message = "Le code projet est obligatoire")
    @Pattern(regexp = "^[A-Z0-9_-]{3,20}$", message = "Code projet : 3-20 caractères majuscules/chiffres/tirets")
    private String codeProjet;

    @NotNull(message = "Le statut est obligatoire")
    private Projet.StatutProjet statut;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin prévisionnelle est obligatoire")
    private LocalDate dateFinPrevue;

    private LocalDate dateFinReelle;

    // CORRECTION : BigDecimal pour les montants financiers
    @DecimalMin(value = "0.0", message = "Le budget ne peut pas être négatif")
    private BigDecimal budgetAlloue;

    @NotNull(message = "Le chef de projet est obligatoire")
    private Long chefProjetId;

    private Set<Long> membresIds;
    private Set<Long> departementIds;
    private Long sucursalId;
}
