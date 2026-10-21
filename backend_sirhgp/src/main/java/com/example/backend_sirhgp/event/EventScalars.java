package com.example.backend_sirhgp.event;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Extraction défensive de valeurs scalaires depuis des entités JPA.
 *
 * ⚠️ RÈGLE ABSOLUE
 * Toute valeur lue sur une entité JPA doit être extraite PAR LE THREAD APPELANT
 * (celui qui publie l'événement), pendant que sa transaction / session Hibernate
 * est encore ouverte. Un listener {@code @Async} ne doit JAMAIS recevoir ni
 * toucher une entité JPA vivante (ni un proxy lazy qui lui appartient).
 *
 * Sinon le thread asynchrone déclenche un lazy-loading concurrent sur la session
 * Hibernate du thread HTTP, ce qui produit :
 * <ul>
 *   <li>{@code IllegalStateException: Illegal pop() with non-matching
 *       JdbcValuesSourceProcessingState} → HTTP 500 sur une requête pourtant valide ;</li>
 *   <li>{@code NonUniqueObjectException} / {@code EntityExistsException} traduits par
 *       Spring en {@code DataIntegrityViolationException} → HTTP 409 Conflict ;</li>
 *   <li>{@code ConcurrentModificationException} dans {@code AbstractFlushingEventListener}.</li>
 * </ul>
 */
@Slf4j
public final class EventScalars {

    private EventScalars() {
    }

    /**
     * Évalue un accesseur sans jamais propager d'exception : un échec de lecture
     * ne doit jamais faire échouer la requête métier en cours.
     */
    public static <T> T value(Supplier<T> accessor, String field) {
        if (accessor == null) {
            return null;
        }
        try {
            return accessor.get();
        } catch (Exception e) {
            log.debug("Valeur '{}' non résolue pour l'événement : {}", field, e.getMessage());
            return null;
        }
    }

    /**
     * Identifiant d'une entité. Sur un proxy Hibernate, {@code getId()} ne
     * déclenche aucune requête SQL : c'est donc l'accès le moins risqué.
     */
    public static Long id(Supplier<Long> accessor) {
        return value(accessor, "id");
    }

    /** Chaîne scalaire (email, nom, libellé, nom d'énum…). */
    public static String text(Supplier<String> accessor, String field) {
        return value(accessor, field);
    }

    /** Date scalaire. */
    public static LocalDate date(Supplier<LocalDate> accessor, String field) {
        return value(accessor, field);
    }

    /** Date-heure scalaire. */
    public static LocalDateTime dateTime(Supplier<LocalDateTime> accessor, String field) {
        return value(accessor, field);
    }

    /** Montant scalaire. */
    public static BigDecimal decimal(Supplier<BigDecimal> accessor, String field) {
        return value(accessor, field);
    }
}