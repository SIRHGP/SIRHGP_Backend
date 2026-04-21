package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Facture;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class FactureRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission;

    @NotNull(message = "La date d'échéance est obligatoire")
    @Future(message = "La date d'échéance doit être dans le futur")
    private LocalDate dateEcheance;

    // CORRECTION : BigDecimal au lieu de Double pour éviter les erreurs d'arrondi
    @NotNull @DecimalMin("0.0")
    private BigDecimal montantHT;

    @NotNull @DecimalMin("0.0")
    private BigDecimal montantTVA;

    @NotNull(message = "Le type de facture est obligatoire")
    private Facture.TypeFacture typeFacture;

    @NotNull(message = "Le projet est obligatoire")
    private Long projetId;

    // emetteurId extrait du token JWT — pas envoyé par le client
    private Long clientId;

    @Valid
    private Set<LigneFactureRequestDTO> lignesFacture;
}