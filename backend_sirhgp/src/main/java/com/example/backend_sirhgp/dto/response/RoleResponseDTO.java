package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoleResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Set<PermissionSummaryDTO> permissions;
    private Long nombreEmployes;
    private Long nombrePostes;

    @Data
    public static class PermissionSummaryDTO {
        private Long id;
        private String name;
    }
}