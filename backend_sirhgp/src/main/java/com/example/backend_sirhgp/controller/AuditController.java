package com.example.backend_sirhgp.controller;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.response.AuditLogResponseDTO;
import com.example.backend_sirhgp.model.AuditLog;
import com.example.backend_sirhgp.response.ApiResponse;
import com.example.backend_sirhgp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'LEAD')")
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * Période de filtrage : DAY (aujourd'hui), WEEK (7 derniers jours), MONTH (30 derniers jours).
     */
    public enum Period {
        DAY, WEEK, MONTH
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getAll(Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findAll(pageable);
        Page<AuditLogResponseDTO> dtoPage = logs.map(this::toDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Journaux d'audit récupérés", dtoPage));
    }

    // 5.9 : détail / utilisateur / entité / stats réservés SUPER_ADMIN + ADMIN
    // (les rôles scopés MANAGER/LEAD n'ont accès qu'aux listes filtrées par entreprise).
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AuditLogResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Journal d'audit récupéré",
                toDTO(auditLogService.findById(id))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) String search,
            // 5.9 : filtre entreprise — LIBRE pour SUPER_ADMIN, FORCÉ côté service pour les autres.
            @RequestParam(required = false) Long entrepriseId,
            Pageable pageable) {

        // Si une période est fournie, calculer automatiquement fromDate/toDate
        if (period != null) {
            LocalDateTime[] range = computePeriodRange(period);
            fromDate = range[0];
            toDate = range[1];
        }

        Page<AuditLog> logs = auditLogService.search(userId, actionType, entityType,
                fromDate, toDate, search, entrepriseId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recherche d'audit effectuée",
                logs.map(this::toDTO)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getByUser(
            @PathVariable Long userId, Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByUserId(userId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Journaux de l'utilisateur récupérés",
                logs.map(this::toDTO)));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getByEntity(
            @PathVariable String entityType, @PathVariable Long entityId, Pageable pageable) {
        Page<AuditLog> logs = auditLogService.findByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Journaux d'entité récupérés",
                logs.map(this::toDTO)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Period period) {

        // Si une période est fournie, calculer automatiquement fromDate/toDate
        if (period != null) {
            LocalDateTime[] range = computePeriodRange(period);
            fromDate = range[0];
            toDate = range[1];
        }

        // Valeurs par défaut : 30 derniers jours
        if (fromDate == null) {
            fromDate = LocalDateTime.now().minusDays(30);
        }
        if (toDate == null) {
            toDate = LocalDateTime.now();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalActions", auditLogService.countActions(fromDate, toDate));
        stats.put("byActionType", auditLogService.getStatsByActionType(fromDate, toDate));
        stats.put("topUsers", auditLogService.getTopActiveUsers(fromDate, toDate, 10));
        return ResponseEntity.ok(new ApiResponse<>(true, "Statistiques d'audit récupérées", stats));
    }

    @GetMapping("/action-types")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActionTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (AuditActionType type : AuditActionType.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", type.name());
            item.put("label", type.getLabel());
            types.add(item);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Types d'actions récupérés", types));
    }

    // ─── 5.9 (§4) — Détection d'anomalies (SUPER_ADMIN global / ADMIN entreprise) ──

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> anomalies() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Anomalies détectées (24 h / 1 h)", auditLogService.anomalies()));
    }

    // ─── 5.9 (§4) — Export des journaux (CSV / JSON, cap 5000 lignes) ──────────

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long entrepriseId,
            @RequestParam(defaultValue = "csv") String format)
            throws java.io.UnsupportedEncodingException,
            com.fasterxml.jackson.core.JsonProcessingException {
        Page<AuditLog> page = auditLogService.exportData(entrepriseId);
        boolean asJson = "json".equalsIgnoreCase(format);
        String body;
        String contentType;
        String filename;
        if (asJson) {
            List<Map<String, Object>> rows = page.getContent().stream().map(this::exportRow).toList();
            body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rows);
            contentType = "application/json;charset=UTF-8";
            filename = "audit.json";
        } else {
            StringBuilder sb = new StringBuilder(
                    "﻿id;timestamp;userEmail;userName;userRole;actionType;description;"
                            + "entityType;entityId;httpMethod;requestPath;httpStatus\n");
            for (AuditLog l : page.getContent()) {
                sb.append(l.getId()).append(';')
                        .append(csv(l.getTimestamp() != null ? l.getTimestamp().toString() : "")).append(';')
                        .append(csv(l.getUserEmail())).append(';')
                        .append(csv(l.getUserName())).append(';')
                        .append(csv(l.getUserRole())).append(';')
                        .append(csv(l.getActionType() != null ? l.getActionType().name() : "")).append(';')
                        .append(csv(l.getActionDescription())).append(';')
                        .append(csv(l.getEntityType())).append(';')
                        .append(l.getEntityId() != null ? l.getEntityId() : "").append(';')
                        .append(csv(l.getHttpMethod())).append(';')
                        .append(csv(l.getRequestPath())).append(';')
                        .append(l.getHttpStatus()).append('\n');
            }
            body = sb.toString();
            contentType = "text/csv;charset=UTF-8";
            filename = "audit.csv";
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    private Map<String, Object> exportRow(AuditLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("timestamp", l.getTimestamp() != null ? l.getTimestamp().toString() : null);
        m.put("userEmail", l.getUserEmail());
        m.put("userName", l.getUserName());
        m.put("userRole", l.getUserRole());
        m.put("actionType", l.getActionType() != null ? l.getActionType().name() : null);
        m.put("description", l.getActionDescription());
        m.put("entityType", l.getEntityType());
        m.put("entityId", l.getEntityId());
        m.put("httpMethod", l.getHttpMethod());
        m.put("requestPath", l.getRequestPath());
        m.put("httpStatus", l.getHttpStatus());
        return m;
    }

    /**
     * Calcule la plage de dates pour une période donnée.
     */
    private LocalDateTime[] computePeriodRange(Period period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from;
        switch (period) {
            case DAY:
                from = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
                break;
            case WEEK:
                from = now.minusDays(7);
                break;
            case MONTH:
            default:
                from = now.minusDays(30);
                break;
        }
        return new LocalDateTime[]{from, now};
    }

    private AuditLogResponseDTO toDTO(AuditLog log) {
        return AuditLogResponseDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .userName(log.getUserName())
                .userRole(log.getUserRole())
                .actionType(log.getActionType())
                .actionLabel(log.getActionType() != null ? log.getActionType().getLabel() : null)
                .actionDescription(log.getActionDescription())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .httpMethod(log.getHttpMethod())
                .requestPath(log.getRequestPath())
                .httpStatus(log.getHttpStatus())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .success(log.isSuccess())
                .errorMessage(log.getErrorMessage())
                .durationMs(log.getDurationMs())
                .timestamp(log.getTimestamp())
                .build();
    }
}