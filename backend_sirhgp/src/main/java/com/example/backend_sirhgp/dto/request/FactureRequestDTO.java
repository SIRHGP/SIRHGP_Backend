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

    @NotNull(message = "Le montant HT est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant HT doit être positif ou nul")
    private BigDecimal montantHT;

    @NotNull(message = "Le montant TVA est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant TVA doit être positif ou nul")
    private BigDecimal montantTVA;

    @NotNull(message = "Le type de facture est obligatoire")
    private Facture.TypeFacture typeFacture;

    @NotNull(message = "Le projet est obligatoire")
    private Long projetId;

    // Client (destinataire) - optionnel lors de la création, sera défini lors du dispatch
    private Long clientId;

    @Valid
    private Set<LigneFactureRequestDTO> lignesFacture;
}