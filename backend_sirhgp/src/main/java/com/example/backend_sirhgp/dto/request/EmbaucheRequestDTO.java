package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Embauche d'un candidat externe (4.23) : création automatique d'un employé.
 * Les champs d'identité (nom/email/téléphone) sont repris de la candidature ;
 * on complète ici le poste, la date d'embauche, le NUI et le salaire.
 */
@Data
public class EmbaucheRequestDTO {

    private Long posteId;          // défaut : poste de l'offre
    private Long departementId;    // optionnel
    private String roleName;       // défaut : STAFF

    private LocalDate dateEmbauche; // défaut : aujourd'hui
    private String nui;
    private BigDecimal salaireBase;
}