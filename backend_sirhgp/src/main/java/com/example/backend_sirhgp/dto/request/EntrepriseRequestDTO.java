// ─── EntrepriseRequestDTO.java ───────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Entreprise;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EntrepriseRequestDTO {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    @Size(max = 255)
    private String name;

    private String description;
    private String secteurActivite;
    private Entreprise.TypeEntreprise type;
    private String adresse;
    private String ville;
    private String telephone;

    @Email(message = "Email invalide")
    private String email;

    private String logoUrl;
    private String rccm;
    private String numeroCnpsEmployeur;
    private Entreprise.StatutEntreprise statut; // ACTIVE / SUSPENDUE (optionnel à la création)
}