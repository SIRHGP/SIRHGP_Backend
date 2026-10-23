package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Evenement;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
public class EvenementRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalTime heureDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private LocalTime heureFin;

    private String lieu;
    private String couleur;

    @NotNull(message = "Le type d'événement est obligatoire")
    private Evenement.TypeEvenement type;

    private boolean rappelActif;
    private Integer minutesRappel;
    private boolean jourEntier;

    // createurId extrait du token JWT
    private Set<Long> participantsIds;
    private Long projetId;
    private Long sucursalId;
}
