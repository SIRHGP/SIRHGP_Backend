package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/** Refonte 5.9 (§6) — jeton d'impersonation + identités pour le bandeau UI. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpersonateResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn; // ms
    private String impersonatorNom;
    private Long cibleId;
    private String cibleNom;
    private String cibleEmail;
    private Instant expireAt;
}