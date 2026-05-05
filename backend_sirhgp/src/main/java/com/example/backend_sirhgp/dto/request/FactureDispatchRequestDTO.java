package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FactureDispatchRequestDTO {

    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    private String message;
}