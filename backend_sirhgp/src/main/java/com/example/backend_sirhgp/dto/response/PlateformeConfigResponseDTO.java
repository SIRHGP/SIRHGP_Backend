package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/** Refonte 5.9 (§5) — paramètre global de configuration de la plateforme. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateformeConfigResponseDTO {
    private Long id;
    private String cle;
    private String valeur;
    private String description;
    private LocalDateTime dateModification;
}