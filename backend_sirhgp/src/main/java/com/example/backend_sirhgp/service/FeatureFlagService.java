package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.response.FeatureFlagResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.FeatureFlag;
import com.example.backend_sirhgp.model.FeatureFlagOverride;
import com.example.backend_sirhgp.repository.EntrepriseRepository;
import com.example.backend_sirhgp.repository.FeatureFlagOverrideRepository;
import com.example.backend_sirhgp.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refonte 5.9 (§5) — feature flags : CRUD SUPER_ADMIN + résolution effective
 * pour l'entreprise de l'utilisateur connecté (GET /api/feature-flags, tous rôles).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagOverrideRepository overrideRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;

    // ─── SUPER_ADMIN — gestion ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FeatureFlagResponseDTO> findAll() {
        return flagRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional
    public FeatureFlagResponseDTO create(String cle, String description, boolean actifParDefaut) {
        String c = cle.trim();
        if (flagRepository.existsByCle(c)) {
            throw new IllegalArgumentException("Feature flag déjà existant : " + c);
        }
        FeatureFlag f = flagRepository.save(FeatureFlag.builder()
                .cle(c).description(description).actifParDefaut(actifParDefaut).build());
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Création du feature flag '" + c + "'", "FeatureFlag", f.getId(), c);
        return toDTO(f);
    }

    @Transactional
    public FeatureFlagResponseDTO update(String key, String description, Boolean actifParDefaut) {
        FeatureFlag f = getFlag(key);
        if (description != null) f.setDescription(description);
        if (actifParDefaut != null) f.setActifParDefaut(actifParDefaut);
        f = flagRepository.save(f);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Modification du feature flag '" + key + "'", "FeatureFlag", f.getId(), key);
        return toDTO(f);
    }

    @Transactional
    public void delete(String key) {
        FeatureFlag f = getFlag(key);
        List<FeatureFlagOverride> overrides = overrideRepository.findByFlagId(f.getId());
        if (!overrides.isEmpty()) overrideRepository.deleteAll(overrides);
        flagRepository.delete(f);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Suppression du feature flag '" + key + "'", "FeatureFlag", f.getId(), key);
    }

    /** Override par tenant : PATCH /api/plateforme/feature-flags/{key}/tenants/{entrepriseId}?actif= */
    @Transactional
    public FeatureFlagResponseDTO overrideTenant(String key, Long entrepriseId, boolean actif) {
        FeatureFlag f = getFlag(key);
        Entreprise e = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable : " + entrepriseId));
        FeatureFlagOverride o = overrideRepository.findByFlagIdAndEntrepriseId(f.getId(), entrepriseId)
                .orElseGet(() -> FeatureFlagOverride.builder().flag(f).entreprise(e).build());
        o.setActif(actif);
        overrideRepository.save(o);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Override du flag '" + key + "' pour l'entreprise '" + e.getName() + "' → " + actif,
                "FeatureFlag", f.getId(), key);
        return toDTO(f);
    }

    // ─── Tous rôles — résolution effective ────────────────────────────────────

    /** Map clé → état effectif pour l'entreprise de l'utilisateur courant. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> effectiveFlags() {
        Map<String, Boolean> out = new LinkedHashMap<>();
        Long entrepriseId = entrepriseContextService.getScopeEntrepriseId();
        for (FeatureFlag f : flagRepository.findAll()) {
            boolean effective = f.isActifParDefaut();
            if (entrepriseId != null) {
                var o = overrideRepository.findByFlagIdAndEntrepriseId(f.getId(), entrepriseId);
                if (o.isPresent()) effective = o.get().isActif();
            }
            out.put(f.getCle(), effective);
        }
        return out;
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private FeatureFlag getFlag(String key) {
        return flagRepository.findByCle(key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag introuvable : " + key));
    }

    private FeatureFlagResponseDTO toDTO(FeatureFlag f) {
        FeatureFlagResponseDTO dto = new FeatureFlagResponseDTO();
        dto.setId(f.getId());
        dto.setCle(f.getCle());
        dto.setDescription(f.getDescription());
        dto.setActifParDefaut(f.isActifParDefaut());
        dto.setOverrides(overrideRepository.findByFlagId(f.getId()).stream()
                .map(o -> new FeatureFlagResponseDTO.OverrideDTO(o.getEntreprise().getId(), o.isActif()))
                .toList());
        return dto;
    }
}
