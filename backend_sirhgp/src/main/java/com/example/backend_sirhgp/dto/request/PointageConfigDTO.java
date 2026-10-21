package com.example.backend_sirhgp.dto.request;

import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

/**
 * Configuration du pointage d'une entreprise (4.2), modifiable par les admins.
 * Tous les champs sont optionnels : null = pas de contrôle sur cette heure.
 */
@Data
public class PointageConfigDTO {

    /** Heure d'arrivée standard : au-delà = retard. */
    @Valid
    private LocalTime arriveeStandard;

    /** Heure de départ standard : avant = départ anticipé. */
    @Valid
    private LocalTime departStandard;

    /** Heure d'arrivée minimale : avant = pointage rédhibitoire/anormal. */
    @Valid
    private LocalTime arriveeMinimale;
}