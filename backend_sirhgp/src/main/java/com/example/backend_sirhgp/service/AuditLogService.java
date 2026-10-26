package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.audit.AuditEvent;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.AuditLog;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.repository.AuditLogRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final EmployerRepository employerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntrepriseContextService entrepriseContextService;

    public void record(Employer actor, AuditActionType actionType, String description,
                       String entityType, Long entityId, String entityName) {
        AuditEvent event = new AuditEvent(this, actor, actionType, description,
                entityType, entityId, entityName);
        eventPublisher.publishEvent(event);
    }

    public void recordFull(Employer actor, AuditActionType actionType, String description,
                           String entityType, Long entityId, String entityName,
                           String httpMethod, String requestPath,
                           Map<String, String> requestParams, String requestBody,
                           int httpStatus, String ipAddress, String userAgent,
                           boolean success, String errorMessage, Long durationMs) {
        AuditEvent event = new AuditEvent(this, actor, actionType, description,
                entityType, entityId, entityName, httpMethod, requestPath,
                requestParams, requestBody, httpStatus, ipAddress, userAgent,
                success, errorMessage, durationMs);
        eventPublisher.publishEvent(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        Pageable sortedPageable = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "timestamp"))
                : pageable;
        // 5.9 (RG-M2-13 / P1-10) : scopage — SUPER_ADMIN = global ; ADMIN = entreprise ;
        // MANAGER = sa succursale ; LEAD = ses départements (cf. EntrepriseContextService).
        if (entrepriseContextService.isSuperAdmin()) {
            return auditLogRepository.findAll(sortedPageable);
        }
        List<Long> actorIds = scopedActorIds();
        return actorIds.isEmpty() ? Page.empty(sortedPageable)
                : auditLogRepository.findByUserIdIn(actorIds, sortedPageable);
    }

    @Transactional(readOnly = true)
    public AuditLog findById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal d'audit introuvable : " + id));
        if (!isSuperAdmin() && (log.getUserId() == null || !scopedActorIds().contains(log.getUserId()))) {
            throw new ResourceNotFoundException("Journal d'audit introuvable : " + id);
        }
        return log;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(Long userId, AuditActionType actionType, String entityType,
                                 LocalDateTime fromDate, LocalDateTime toDate,
                                 String search, Long entrepriseId, Pageable pageable) {
        // 5.9 : le filtre entreprise est LIBRE pour le SUPER_ADMIN, FORCÉ à la propre
        // entreprise pour tous les autres rôles (le paramètre demandé est ignoré).
        Long effective;
        if (entrepriseContextService.isSuperAdmin()) {
            effective = entrepriseId;
        } else {
            effective = entrepriseContextService.getScopeEntrepriseId();
            if (effective == null) return Page.empty(pageable);
        }

        String role = entrepriseContextService.currentRole();
        if ("MANAGER".equals(role) && entrepriseContextService.getScopeSucursalId() == null) {
            return Page.empty(pageable);
        }
        if ("LEAD".equals(role) && scopeDepartementIds().isEmpty()) {
            return Page.empty(pageable);
        }

        // 5.9 (RG-M2-13 / P1-10) : au-delà du tenant, les rôles de terrain sont
        // restreints à leur périmètre fin — MANAGER = succursale, LEAD = départements.
        Long sucursalId = scopeSucursalId();
        if (sucursalId != null) {
            return auditLogRepository.search(userId, actionType, entityType,
                    fromDate, toDate, search, effective, sucursalId, pageable);
        }
        List<Long> departementIds = scopeDepartementIds();
        if (!departementIds.isEmpty()) {
            return auditLogRepository.searchByDepartementIds(userId, actionType, entityType,
                    fromDate, toDate, search, effective, departementIds, pageable);
        }
        return auditLogRepository.search(userId, actionType, entityType,
                fromDate, toDate, search, effective, null, pageable);
    }

    /**
     * 5.9 (P1-10) : périmètre MANAGER — id de succursale à appliquer en filtre, ou
     * null quand aucun scoping fin ne s'applique (rôle ≠ MANAGER, ou MANAGER sans
     * succursale de rattachement : il retombe alors sur le périmètre entreprise).
     */
    private Long scopeSucursalId() {
        if (!"MANAGER".equals(entrepriseContextService.currentRole())) return null;
        return entrepriseContextService.getScopeSucursalId();
    }

    /**
     * 5.9 (P1-10) : périmètre LEAD — ids de départements à appliquer en filtre.
     * Vide = aucun scoping fin (rôle ≠ LEAD, ou LEAD sans ancrage départemental) :
     * le périmètre entreprise s'applique alors, comme avant cette extension.
     * Le test de rôle est porté par `EntrepriseContextService.getScopeDepartementIds()`.
     */
    private List<Long> scopeDepartementIds() {
        return entrepriseContextService.getScopeDepartementIds();
    }

    // ─── 5.9 (§4) — Export des journaux (CSV / JSON, cap 5000) ────────────────

    @Transactional(readOnly = true)
    public Page<AuditLog> exportData(Long entrepriseId) {
        Pageable p = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "timestamp"));
        return search(null, null, null, null, null, null, entrepriseId, p);
    }

    // ─── 5.9 (§4) — Détection d'anomalies ─────────────────────────────────────

    /**
     * Règles v1 : ≥5 échecs de connexion / 24 h par email ; pic de suppressions
     * (≥20 en 1 h) et de modifications (≥100 en 1 h). Portée : SUPER_ADMIN global,
     * ADMIN = son entreprise. 5.9 : endpoint réservé à SUPER_ADMIN/ADMIN
     * (`@PreAuthorize` sur `AuditController`) — pas de scoping fin MANAGER/LEAD ici.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> anomalies() {
        boolean sa = entrepriseContextService.isSuperAdmin();
        Long own = entrepriseContextService.getScopeEntrepriseId();
        if (!sa && own == null) {
            return Map.of("loginFailures", List.of(), "deletesLastHour", 0L,
                    "updatesLastHour", 0L, "deleteSpike", false, "updateSpike", false);
        }
        LocalDateTime from24h = LocalDateTime.now().minusHours(24);
        LocalDateTime from1h = LocalDateTime.now().minusHours(1);

        List<Object[]> rows = sa
                ? auditLogRepository.loginFailuresSince(AuditActionType.LOGIN_FAILED, from24h, 5)
                : auditLogRepository.loginFailuresByEntreprise(AuditActionType.LOGIN_FAILED, from24h, own, 5);
        List<Map<String, Object>> failures = rows.stream()
                .map(r -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("email", r[0]);
                    m.put("count", r[1]);
                    return m;
                })
                .toList();

        long deletes = sa
                ? auditLogRepository.countByActionTypeAndTimestampAfter(AuditActionType.DELETE, from1h)
                : auditLogRepository.countByActionTypeSinceForEntreprise(AuditActionType.DELETE, from1h, own);
        long updates = sa
                ? auditLogRepository.countByActionTypeAndTimestampAfter(AuditActionType.UPDATE, from1h)
                : auditLogRepository.countByActionTypeSinceForEntreprise(AuditActionType.UPDATE, from1h, own);

        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("loginFailures", failures);
        out.put("deletesLastHour", deletes);
        out.put("updatesLastHour", updates);
        out.put("deleteSpike", deletes >= 20);
        out.put("updateSpike", updates >= 100);
        out.put("seuilLoginFailures24h", 5);
        out.put("seuilDeleteHoraire", 20);
        out.put("seuilUpdateHoraire", 100);
        return out;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByUserId(Long userId, Pageable pageable) {
        if (isSuperAdmin()) return auditLogRepository.findByUserId(userId, pageable);
        List<Long> actorIds = scopedActorIds();
        return actorIds.contains(userId) ? auditLogRepository.findByUserId(userId, pageable)
                : Page.empty(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(String entityType, Long entityId, Pageable pageable) {
        if (isSuperAdmin()) return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        List<Long> actorIds = scopedActorIds();
        return actorIds.isEmpty() ? Page.empty(pageable)
                : auditLogRepository.findByEntityTypeAndEntityIdAndUserIdIn(entityType, entityId, actorIds, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getScopedStats(LocalDateTime fromDate, LocalDateTime toDate, int limit) {
        boolean global = isSuperAdmin();
        List<Long> actorIds = global ? null : scopedActorIds();
        long total = global ? auditLogRepository.countByTimestampBetween(fromDate, toDate)
                : actorIds.isEmpty() ? 0 : auditLogRepository.countByUserIdsAndTimestampBetween(actorIds, fromDate, toDate);
        List<Object[]> actionRows = global ? auditLogRepository.countByActionTypeBetween(fromDate, toDate)
                : actorIds.isEmpty() ? List.of()
                    : auditLogRepository.countByActionTypeBetweenForUsers(actorIds, fromDate, toDate);
        Map<AuditActionType, Long> byAction = actionRows.stream().collect(Collectors.toMap(
                row -> (AuditActionType) row[0], row -> (Long) row[1]));
        List<Object[]> topUsers = global ? auditLogRepository.topActiveUsers(fromDate, toDate, PageRequest.of(0, limit))
                : actorIds.isEmpty() ? List.of()
                    : auditLogRepository.topActiveUsersForUsers(actorIds, fromDate, toDate, PageRequest.of(0, limit));
        return Map.of("totalActions", total, "byActionType", byAction, "topUsers", topUsers);
    }

    private boolean isSuperAdmin() {
        return entrepriseContextService.isSuperAdmin();
    }

    /** Liste d'acteurs dans le périmètre strict de l'utilisateur courant. */
    private List<Long> scopedActorIds() {
        Long entrepriseId = entrepriseContextService.getScopeEntrepriseId();
        if (entrepriseId == null) return List.of();
        String role = entrepriseContextService.currentRole();
        if (role == null) return List.of();
        return switch (role) {
            case "ADMIN" -> employerRepository.findIdsByEntrepriseId(entrepriseId);
            case "MANAGER" -> {
                Long sucursalId = entrepriseContextService.getScopeSucursalId();
                yield sucursalId == null ? List.of()
                        : employerRepository.findIdsByEntrepriseIdAndSucursalId(entrepriseId, sucursalId);
            }
            case "LEAD" -> {
                List<Long> departments = scopeDepartementIds();
                yield departments.isEmpty() ? List.of()
                        : employerRepository.findIdsByEntrepriseIdAndDepartementIds(entrepriseId, departments);
            }
            default -> List.of();
        };
    }

    @Transactional(readOnly = true)
    public Map<AuditActionType, Long> getStatsByActionType(LocalDateTime fromDate, LocalDateTime toDate) {
        return auditLogRepository.countByActionTypeBetween(fromDate, toDate)
                .stream()
                .collect(Collectors.toMap(
                        row -> (AuditActionType) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopActiveUsers(LocalDateTime fromDate, LocalDateTime toDate, int limit) {
        return auditLogRepository.topActiveUsers(fromDate, toDate, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long countActions(LocalDateTime fromDate, LocalDateTime toDate) {
        return auditLogRepository.countByTimestampBetween(fromDate, toDate);
    }
}
