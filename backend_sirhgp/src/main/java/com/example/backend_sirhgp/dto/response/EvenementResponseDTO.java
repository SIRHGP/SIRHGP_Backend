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

    // Créateur : résumé
    private ProjetResponseDTO.EmployerSummaryDTO createur;

    // Participants : résumés
    private Set<ProjetResponseDTO.EmployerSummaryDTO> participants;

    // Projet lié : résumé
    private TacheResponseDTO.ProjetSummaryDTO projet;

    // Indicateurs calculés
    private Long nombreParticipants;
    private Boolean estPasse;
    private Boolean estEnCours;
    private Boolean estAvenir;
    private Long dureeMinutes;
}