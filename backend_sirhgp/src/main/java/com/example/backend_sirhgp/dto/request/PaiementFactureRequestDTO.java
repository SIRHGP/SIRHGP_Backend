package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.PaiementFacture;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaiementFactureRequestDTO {

    @NotNull(message = "Le mode de paiement est obligatoire")
    private PaiementFacture.ModePaiement mode;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être strictement positif")
    private BigDecimal montant;

    /** Date du règlement (par défaut : aujourd'hui). */
    private LocalDate datePaiement;

    /** Référence du règlement : n° de chèque, référence de virement… */
    private String reference;

    private String note;
}
