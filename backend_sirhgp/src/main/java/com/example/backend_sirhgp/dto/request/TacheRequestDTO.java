package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Tache;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TacheRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "Le statut est obligatoire")
    private Tache.StatutTache statut;

    @NotNull(message = "La priorité est obligatoire")
    private Tache.Priorite priorite;

    @Future(message = "La date d'échéance doit être dans le futur")
    private LocalDate dateEcheance;

    private Long assigneeId;  // Nullable : tâche non assignée à la création

    @NotNull(message = "Le projet est obligatoire")
    private Long projetId;

    private Long sucursalId;
}
