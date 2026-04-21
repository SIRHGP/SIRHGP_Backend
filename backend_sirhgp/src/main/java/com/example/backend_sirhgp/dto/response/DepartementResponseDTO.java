package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class DepartementResponseDTO {
    private Long id;
    private String name;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long nombreEmployes;
    private Long nombrePostes;
    private Long nombreProjets;
    private Set<PosteSummaryDTO> postes;

    @Data
    public static class PosteSummaryDTO {
        private Long id;
        private String name;
        private Long nombreEmployes;
    }
}