package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Tache;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CORRECTION : commentairesRecents passe de Set à List.
 * findRecentsByTacheId() retourne une List (ordre garanti par date DESC).
 * Un Set ne garantit pas l'ordre — pour les commentaires récents ordonnés,
 * List est le bon type.
 */
@Data
public class TacheResponseDTO {

    private Long id;
    private String titre;
    private String description;
    private Tache.StatutTache statut;
    private Tache.Priorite priorite;
    private LocalDate dateEcheance;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    private ProjetResponseDTO.EmployerSummaryDTO assignee;
    private Long assigneeId;
    private ProjetResponseDTO.EmployerSummaryDTO createur;
    private Long createurId;
    private ProjetSummaryDTO projet;
    private Long projetId;
    private Long sucursalId;
    private String sucursalNom;
    private Tache.Visibilite visibilite;

    private Long nombreCommentaires;
    // CORRECTION : List (ordonné) au lieu de Set (non ordonné)
    private List<CommentaireResponseDTO> commentairesRecents;
    private Boolean estEnRetard;
    private Long joursRestants;
    private Long dureeExecution;
    private Double pourcentageCompletion;

    @Data
    public static class ProjetSummaryDTO {
        private Long id;
        private String nom;
        private String codeProjet;
    }
}
