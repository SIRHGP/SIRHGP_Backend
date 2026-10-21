package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.PaiementFacture;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaiementFactureResponseDTO {

    private Long id;
    private Long factureId;
    private PaiementFacture.ModePaiement mode;
    private BigDecimal montant;
    private LocalDate datePaiement;
    private String reference;
    private String note;
    private String compteComptable;
    private Long enregistreParId;
    private String enregistreParNom;
    private LocalDateTime dateCreation;
}
