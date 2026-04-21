package com.example.backend_sirhgp.dto.response;
import lombok.Data;

import java.time.LocalDateTime;


// ─── ActionResponseDTO ───────────────────────────────────────────────────────
@Data
public class ActionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long nombrePermissions;
}