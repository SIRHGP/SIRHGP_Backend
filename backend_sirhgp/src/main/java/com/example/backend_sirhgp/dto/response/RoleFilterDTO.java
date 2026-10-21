package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Refonte 5.9 (§3) — entrée légère de GET /api/roles/filtres (liste non sensible pour les filtres UI). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleFilterDTO {
    private Long id;
    private String name;
}