package com.example.backend_sirhgp.audit;

import com.example.backend_sirhgp.model.AuditLog;
import com.example.backend_sirhgp.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    /**
     * Enregistre l'entrée d'audit dans une transaction PROPRE (nouvelle session
     * Hibernate).
     *
     * ⚠️ Ce listener tourne dans un thread @Async : il ne doit JAMAIS réutiliser
     * ni toucher la session Hibernate du thread HTTP, sous peine de corrompre le
     * flush en cours (ConcurrentModificationException, NPE dans
     * AbstractFlushingEventListener, "Illegal pop() with non-matching
     * JdbcValuesSourceProcessingState") et donc de faire échouer des requêtes
     * HTTP parfaitement valides.
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = buildAuditLog(event);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du journal d'audit", e);
        }
    }

    private AuditLog buildAuditLog(AuditEvent event) {
        // Aucun accès à une entité JPA : l'événement ne transporte que des
        // valeurs scalaires extraites dans le thread HTTP (voir AuditEvent).
        return AuditLog.builder()
                .userId(event.getActorId())
                .userEmail(event.getActorEmail())
                .userName(event.getActorName())
                .userRole(event.getActorRole())
                .actionType(event.getActionType())
                .actionDescription(event.getDescription())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .entityName(event.getEntityName())
                .httpMethod(event.getHttpMethod())
                .requestPath(event.getRequestPath())
                .requestParams(serializeParams(event.getRequestParams()))
                .requestBody(event.getRequestBody())
                .httpStatus(event.getHttpStatus())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .success(event.isSuccess())
                .errorMessage(event.getErrorMessage())
                .durationMs(event.getDurationMs())
                .timestamp(event.getOccurredAt())
                .build();
    }

    private String serializeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
