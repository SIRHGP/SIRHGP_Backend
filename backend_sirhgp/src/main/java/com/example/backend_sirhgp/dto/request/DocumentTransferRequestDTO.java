package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentTransferRequestDTO {

    @NotNull(message = "L'identifiant du destinataire est obligatoire")
    private Long recipientId;

    private String message; // optionnel
}