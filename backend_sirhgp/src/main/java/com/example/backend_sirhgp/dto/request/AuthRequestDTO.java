package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de requête pour l'authentification.
 * Utilisé UNIQUEMENT en entrée (login), jamais en réponse.
 */
@Data
public class AuthRequestDTO {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}