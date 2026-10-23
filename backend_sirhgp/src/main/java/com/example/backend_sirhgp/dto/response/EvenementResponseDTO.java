package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Evenement;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
public class EvenementResponseDTO {

    private Long id;
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalTime heureDebut;
    private LocalDate dateFin;
    private LocalTime heureFin;
    private String lieu;
    private String couleur;
    private Evenement.TypeEvenement type;
    private boolean rappelActif;
    private Integer minutesRappel;
    private boolean jourEntier;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long entrepriseId;
    private Long sucursalId;
    private String sucursalNom;

    // ⭐ CHAMPS AJOUTÉS POUR LE FRONTEND ⭐
    private Long createurId;        // ID du créateur (pour comparaison)
    private String createurNom;     // Nom du créateur (pour affichage)

    // Créateur : résumé complet
    private EmployerSummaryDTO createur;

    // Participants : résumés
    private Set<EmployerSummaryDTO> participants;

    // Projet lié : résumé
    private ProjetSummaryDTO projet;

    // Indicateurs calculés
    private Long nombreParticipants;
    private Boolean estPasse;
    private Boolean estEnCours;
    private Boolean estAvenir;
    private Long dureeMinutes;

    // ─── DTOs internes ─────────────────────────────────────────

    @Data
    public static class EmployerSummaryDTO {
        private Long id;
        private String name;
        private String email;
        private String posteName;
        private String departementNom;
    }

    @Data
    public static class ProjetSummaryDTO {
        private Long id;
        private String nom;
        private String codeProjet;
    }
}
