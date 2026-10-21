// ─── AuthEntrepriseDTO.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

/**
 * Entreprise simplifiée pour l'écran de connexion (public).
 */
public record AuthEntrepriseDTO(Long id, String nom, String logoUrl) {
}