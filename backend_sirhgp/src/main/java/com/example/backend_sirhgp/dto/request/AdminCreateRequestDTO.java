package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Refonte 5.9 (§2) — création d'un compte ADMIN supplémentaire par le SUPER_ADMIN :
 * POST /api/entreprises/{id}/admins
 * Le rôle est FORCÉ à ADMIN côté service (jamais de création via /api/employers).
 */
@Data
public class AdminCreateRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;
}