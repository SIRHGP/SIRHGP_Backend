package com.example.backend_sirhgp.audit;

import com.example.backend_sirhgp.model.Employer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Événement d'audit publié dans le contexte Spring pour être traité
 * de manière asynchrone par {@link AuditEventListener}.
 *
 * ⚠️ RÈGLE IMPORTANTE
 * Cet événement est consommé par un listener {@code @Async}, donc exécuté dans
 * un AUTRE thread que celui de la requête HTTP. Il ne doit donc JAMAIS
 * transporter d'entité JPA vivante (proxy lazy attaché à la session Hibernate
 * du thread HTTP) : y accéder depuis le thread asynchrone pendant que le thread
 * HTTP effectue son flush corrompt la session en cours
 * (ConcurrentModificationException, "Illegal pop() with non-matching
 * JdbcValuesSourceProcessingState", NullPointerException dans
 * AbstractFlushingEventListener), ce qui faisait échouer des requêtes HTTP
 * parfaitement valides (409/500 aléatoires).
 *
 * Le constructeur accepte toujours un {@link Employer} pour ne pas changer les
 * points d'appel, mais il en extrait immédiatement les valeurs scalaires
 * (id, email, nom, rôle) : le thread asynchrone ne manipule plus que des
 * données détachées et immuables.
 */
@Slf4j
@Getter
public class AuditEvent extends ApplicationEvent {

    private final AuditActionType actionType;
    private final String description;
    private final String entityType;
    private final Long entityId;
    private final String entityName;
    private final String httpMethod;
    private final String requestPath;
    private final Map<String, String> requestParams;
    private final String requestBody;
    private final int httpStatus;
    private final String ipAddress;
    private final String userAgent;
    private final boolean success;
    private final String errorMessage;
    private final Long durationMs;
    /** Acteur sous forme de valeurs scalaires (jamais d'entité JPA vivante). */
    private final Long actorId;
    private final String actorEmail;
    private final String actorName;
    private final String actorRole;
    private final LocalDateTime occurredAt;

    /**
     * Constructeur complet.
     */
    public AuditEvent(Object source,
                      Employer actor,
                      AuditActionType actionType,
                      String description,
                      String entityType,
                      Long entityId,
                      String entityName,
                      String httpMethod,
                      String requestPath,
                      Map<String, String> requestParams,
                      String requestBody,
                      int httpStatus,
                      String ipAddress,
                      String userAgent,
                      boolean success,
                      String errorMessage,
                      Long durationMs) {
        super(source);
        // ✅ Extraction IMMÉDIATE dans le thread appelant (transaction encore active) :
        // le thread @Async ne touchera plus jamais à l'entité Employer.
        this.actorId = safeId(actor);
        this.actorEmail = safeString(() -> actor.getEmail(), "email");
        this.actorName = safeString(() -> actor.getName(), "name");
        this.actorRole = safeString(() -> {
            if (actor.getRole() == null) return null;
            return actor.getRole().getName();
        }, "role");
        this.actionType = actionType;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.requestParams = requestParams;
        this.requestBody = requestBody;
        this.httpStatus = httpStatus;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.occurredAt = LocalDateTime.now();
    }

    /**
     * Constructeur simplifié pour un usage programmatique.
     */
    public AuditEvent(Object source, Employer actor, AuditActionType actionType,
                      String description, String entityType, Long entityId, String entityName) {
        this(source, actor, actionType, description, entityType, entityId, entityName,
                null, null, null, null, 200, null, null, true, null, null);
    }
    /**
     * Récupère l'identifiant de l'acteur sans risque : sur un proxy Hibernate,
     * {@code getId()} ne déclenche aucune requête SQL.
     */
    private static Long safeId(Employer actor) {
        if (actor == null) {
            return null;
        }
        try {
            return actor.getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Évalue un accesseur de l'entité de façon défensive : si la valeur n'est pas
     * résoluble (entité détachée, proxy lazy non initialisé, hors transaction),
     * on renvoie {@code null} au lieu de propager l'exception.
     * L'audit ne doit jamais faire échouer la requête métier.
     */
    private static String safeString(java.util.function.Supplier<String> accessor, String field) {
        try {
            return accessor.get();
        } catch (Exception e) {
            log.debug("Champ '{}' de l'acteur non résolu pour l'audit : {}", field, e.getMessage());
            return null;
        }
    }

}