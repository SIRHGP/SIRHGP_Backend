package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO pour le transfert d'un document existant vers un autre employé.
 */
@Data
public class DocumentTransferRequestDTO {

    @NotNull(message = "L'identifiant du destinataire est obligatoire")
    private Long destinataireId;

    // Message optionnel accompagnant le transfert
    private String message;
}