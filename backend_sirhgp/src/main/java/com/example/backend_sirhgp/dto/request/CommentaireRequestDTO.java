package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentaireRequestDTO {

    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu;

    @NotNull(message = "L'ID de la tâche est obligatoire")
    private Long tacheId;

    // auteurId retiré : doit être extrait du token JWT côté service, pas envoyé par le client
}