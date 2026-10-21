// ─── PointageResponseDTO.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pointage exposé à l'employé courant (badgeuse, 4.1).
 * dureeMinutes est calculé côté serveur (départ − arrivée), null si départ non pointé.
 */
@Data
public class PointageResponseDTO {

    private Long id;
    private LocalDate date;
    private LocalDateTime arrivee;
    private LocalDateTime depart;
    private boolean corrige;
    private Long dureeMinutes;
    private String jourSemaine; // LUNDI..DIMANCHE (calculé)

    // Règles calculées depuis la config pointage de l'entreprise (4.2)
    private boolean retarde;          // arrivée après l'heure d'arrivée standard
    private boolean departAnticipe;   // départ avant l'heure de départ standard
    private boolean arriveeAnormale;  // arrivée avant l'heure d'arrivée minimale (rédhibitoire)
}