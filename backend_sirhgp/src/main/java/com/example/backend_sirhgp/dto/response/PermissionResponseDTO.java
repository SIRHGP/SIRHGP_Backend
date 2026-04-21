package com.example.backend_sirhgp.dto.response;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

// ─── PermissionResponseDTO ───────────────────────────────────────────────────
@Data
public class PermissionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    // Actions liées : résumé (id + nom suffisent)
    private Set<ActionSummaryDTO> actions;
    private Long nombreRoles;
    private Long nombrePostes;

    @Data
    public static class ActionSummaryDTO {
        private Long id;
        private String name;
    }
}