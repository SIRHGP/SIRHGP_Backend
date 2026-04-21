package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LigneFactureRequestDTO {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    @NotNull @DecimalMin(value = "0.0", message = "Le prix unitaire ne peut pas être négatif")
    private BigDecimal prixUnitaire;

    private Long tacheId; // optionnel : lier la ligne à une tâche
}