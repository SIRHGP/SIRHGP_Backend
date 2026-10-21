package com.example.backend_sirhgp.audit;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur HTTP qui capture automatiquement toutes les requêtes
 * entrantes et les enregistre dans le journal d'audit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    private static final String[] IGNORED_PATHS = {
            "/api/auth/login",
            "/api/auth/profile",
            "/api/auth/logout",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/actuator",
            "/error",
            "/static",
            "/css",
            "/js"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("audit_start_time", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String path = request.getRequestURI();
            if (shouldIgnore(path)) {
                return;
            }

            long startTime = (long) request.getAttribute("audit_start_time");
            long duration = System.currentTimeMillis() - startTime;

            Employer actor = getCurrentUser();
            AuditActionType actionType = mapHttpMethod(request.getMethod());
            Long entityId = extractEntityId(path);
            String entityType = extractEntityType(path);
            boolean success = response.getStatus() < 400;

            auditLogService.recordFull(
                    actor,
                    actionType,
                    buildDescription(actionType, entityType, entityId),
                    entityType,
                    entityId,
                    null,
                    request.getMethod(),
                    path,
                    getRequestParams(request),
                    null,
                    response.getStatus(),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    success,
                    ex != null ? ex.getMessage() : (success ? null : "HTTP " + response.getStatus()),
                    duration
            );
        } catch (Exception e) {
            log.warn("Erreur lors de l'audit de la requête : {}", e.getMessage());
        }
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Employer employer) {
            return employer;
        }
        return null;
    }

    private boolean shouldIgnore(String path) {
        for (String ignored : IGNORED_PATHS) {
            if (path.startsWith(ignored)) {
                return true;
            }
        }
        return false;
    }

    private AuditActionType mapHttpMethod(String method) {
        return switch (method.toUpperCase()) {
            case "POST" -> AuditActionType.CREATE;
            case "PUT", "PATCH" -> AuditActionType.UPDATE;
            case "DELETE" -> AuditActionType.DELETE;
            case "GET" -> AuditActionType.READ;
            default -> AuditActionType.OTHER;
        };
    }

    private Long extractEntityId(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException ignored) {
                // continue
            }
        }
        return null;
    }

    private String extractEntityType(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            return normalizeEntityType(parts[2]);
        }
        return path;
    }

    /**
     * Normalise le nom d'entité extrait de l'URL :
     * - retire le 's' final (pluriel → singulier)
     * - met la première lettre en majuscule
     * Exemple : "factures" → "Facture", "documents" → "Document"
     */
    private String normalizeEntityType(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String singular = raw.endsWith("s") ? raw.substring(0, raw.length() - 1) : raw;
        return singular.substring(0, 1).toUpperCase() + singular.substring(1);
    }

    private String buildDescription(AuditActionType type, String entityType, Long entityId) {
        String label = type.getLabel();
        if (entityType != null) {
            String entityName = entityType.substring(0, 1).toUpperCase() + entityType.substring(1);
            if (entityId != null) {
                return label + " de " + entityName + " (id=" + entityId + ")";
            }
            return label + " de " + entityName;
        }
        return label;
    }

    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params.isEmpty() ? null : params;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}