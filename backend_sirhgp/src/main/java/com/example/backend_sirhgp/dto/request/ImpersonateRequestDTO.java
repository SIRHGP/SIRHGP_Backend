package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Refonte 5.9 (§6) — POST /api/impersonate : cible de l'impersonation (lecture seule, jeton 15 min). */
@Data
public class ImpersonateRequestDTO {
    @NotNull(message = "L'identifiant de l'utilisateur cible est obligatoire")
    private Long employerId;
}