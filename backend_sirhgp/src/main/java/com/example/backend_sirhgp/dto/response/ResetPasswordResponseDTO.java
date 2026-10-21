package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Refonte 5.9 (§2) — réponse du reset de mot de passe : mot de passe temporaire à transmettre hors audit. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponseDTO {
    private Long employerId;
    private String email;
    /** Mot de passe temporaire généré (le drapeau motDePasseTemporaire est levé sur le compte). */
    private String motDePasseTemporaire;
}