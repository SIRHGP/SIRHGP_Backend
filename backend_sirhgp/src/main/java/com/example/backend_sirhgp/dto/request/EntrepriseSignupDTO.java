// ─── EntrepriseSignupDTO.java ────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Entreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête publique d'inscription d'une nouvelle entreprise depuis le login.
 * Crée l'entreprise + le compte administrateur de cette entreprise en une transaction.
 */
@Data
public class EntrepriseSignupDTO {

    // ─── Entreprise ───────────────────────────────────────────
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(max = 255)
    private String entrepriseNom;

    private String secteurActivite;
    private Entreprise.TypeEntreprise type;
    private String ville;
    private String telephone;

    // ─── Compte administrateur ────────────────────────────────
    @NotBlank(message = "Le nom de l'administrateur est obligatoire")
    @Size(max = 255)
    private String adminNom;

    @NotBlank(message = "L'email de l'administrateur est obligatoire")
    @Email(message = "Format d'email invalide")
    private String adminEmail;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;
}