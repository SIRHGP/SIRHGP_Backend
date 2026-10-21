package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.EcritureComptable;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EcritureComptableResponseDTO {

    private Long id;
    private Long factureId;
    private String numeroFacture;
    private EcritureComptable.Journal journal;
    private LocalDate date;
    private String compte;
    private String libelle;
    private BigDecimal debit;
    private BigDecimal credit;
    private LocalDateTime dateCreation;
}
