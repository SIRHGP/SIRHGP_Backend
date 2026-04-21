package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * CORRECTION : PosteResponseDTO utilise sa propre inner class PermissionSummaryDTO
 * au lieu de référencer PermissionResponseDTO.PermissionSummaryDTO qui causait
 * l'erreur "cannot find symbol".
 * Chaque DTO possède ses propres classes résumé imbriquées pour être autonome.
 */
@Data
public class PosteResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long roleId;
    private String roleName;
    private Long departementId;
    private String departementName;
    private Set<PermissionSummaryDTO> permissions;
    private Long nombreEmployes;

    @Data
    public static class PermissionSummaryDTO {
        private Long id;
        private String name;
    }
}