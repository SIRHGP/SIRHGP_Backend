package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Refonte 5.9 (§6) — GET /api/impersonate/me : état en cours pour le bandeau UI. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpersonateMeDTO {
    private boolean actif;
    private String impersonatorNom;
    private String cibleNom;
    private String cibleEmail;
}