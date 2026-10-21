// ─── BulletinPaieResponseDTO.java ────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BulletinPaieResponseDTO {

    private Long id;
    private Long employeId;
    private String employeNom;
    private String posteNom;
    private String nui;
    private Long departementId;
    private String departementNom;
    private Integer mois;
    private Integer annee;

    private BigDecimal salaireBase;
    private BigDecimal heuresTravaillees;
    private BigDecimal heuresSupp;
    private BigDecimal montantHeuresSupp;
    private BigDecimal primes;
    private BigDecimal indemnites;
    private BigDecimal brut;

    private BigDecimal cnpsSalariale;
    private BigDecimal cnpsPatronale;
    private BigDecimal irpp;
    private BigDecimal pvid;
    private BigDecimal cfc;
    private BigDecimal tpr;
    private BigDecimal totalRetenues;

    private BigDecimal netAPayer;
    private BigDecimal coutTotalEmployeur;
    private LocalDateTime dateCreation;
}