package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/** Refonte 5.9 (§5) — feature flag + overrides par tenant. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagResponseDTO {
    private Long id;
    private String cle;
    private String description;
    private boolean actifParDefaut;
    private List<OverrideDTO> overrides = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverrideDTO {
        private Long entrepriseId;
        private boolean actif;
    }
}