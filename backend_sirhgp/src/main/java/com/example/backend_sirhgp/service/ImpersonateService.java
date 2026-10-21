package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.response.ImpersonateMeDTO;
import com.example.backend_sirhgp.dto.response.ImpersonateResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Refonte 5.9 (§6) — impersonation d'un utilisateur par le SUPER_ADMIN (support).
 *
 * Garde-fous :
 * - jeton COURT (15 min) portant l'identité de la cible + claim impersonatorId ;
 * - le jeton impersonné est LECTURE SEULE (JwtAuthenticationFilter refuse les méthodes
 *   non-GET hors /api/impersonate et /api/auth/logout) ;
 * - il ne possède jamais le rôle SUPER_ADMIN (cible ≠ SUPER_ADMIN) → /api/plateforme/**,
 *   écritures /api/entreprises/** et mutations /api/roles/** restent refusées ;
 * - toute impersonation est journalisée avec l'identité réelle ET la cible ;
 * - logout = blacklisting du jeton impersonné.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImpersonateService {

    /** Durée de vie du jeton d'impersonation (15 minutes). */
    public static final long IMPLICIT_EXPIRATION_MS = 15 * 60 * 1000L;

    private final EmployerRepository employerRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;

    @Transactional
    public ImpersonateResponseDTO impersonate(Long targetId) {
        if (!entrepriseContextService.isSuperAdmin()) {
            throw new AccessDeniedException("Impersonation réservée au SUPER_ADMIN");
        }
        Employer real = entrepriseContextService.getCurrentEmployer();
        if (real == null) throw new AccessDeniedException("Compte introuvable");

        Employer target = employerRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur cible introuvable : " + targetId));

        if (target.getRole() != null && "SUPER_ADMIN".equals(target.getRole().getName())) {
            throw new IllegalArgumentException("Impossible d'impersonner un SUPER_ADMIN");
        }
        if (!target.isActif()) {
            throw new IllegalArgumentException("Impossible d'impersonner un compte désactivé");
        }
        if (real.getId().equals(target.getId())) {
            throw new IllegalArgumentException("La cible est votre propre compte");
        }

        String token = jwtService.generateImpersonationToken(real, target, IMPLICIT_EXPIRATION_MS);

        // Audit : identité réelle + cible
        auditLogService.record(real, AuditActionType.IMPERSONATE,
                "SUPER_ADMIN '" + real.getName() + "' a pris le contrôle de '"
                        + target.getName() + "' (" + target.getEmail() + ", id=" + target.getId() + ")",
                "Employer", target.getId(), target.getName());

        log.info("Impersonation : {} → {}", real.getEmail(), target.getEmail());
        return new ImpersonateResponseDTO(token, "Bearer", IMPLICIT_EXPIRATION_MS,
                real.getName(), target.getId(), target.getName(), target.getEmail(),
                Instant.now().plusMillis(IMPLICIT_EXPIRATION_MS));
    }

    /** État en cours (depuis les claims du jeton impersonné, injectés par le filtre). */
    @Transactional(readOnly = true)
    public ImpersonateMeDTO me(Long impersonatorId, Long cibleId) {
        if (impersonatorId == null || cibleId == null) {
            return new ImpersonateMeDTO(false, null, null, null);
        }
        String impersonatorNom = employerRepository.findById(impersonatorId)
                .map(Employer::getName).orElse("Super Admin");
        Employer cible = employerRepository.findById(cibleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cible introuvable"));
        return new ImpersonateMeDTO(true, impersonatorNom, cible.getName(), cible.getEmail());
    }

    /** Sortie : révoque le jeton impersonné (blacklist) + audit. */
    @Transactional
    public void logout(String bearerToken, Long impersonatorId, Long cibleId) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            tokenBlacklistService.blacklistToken(bearerToken);
        }
        Employer real = impersonatorId != null
                ? employerRepository.findById(impersonatorId).orElse(null)
                : entrepriseContextService.getCurrentEmployer();
        Employer target = cibleId != null ? employerRepository.findById(cibleId).orElse(null) : null;
        auditLogService.record(real, AuditActionType.STOP_IMPERSONATE,
                "Fin de l'impersonation de '" + (target != null ? target.getName() : "cible inconnue")
                        + "' par '" + (real != null ? real.getName() : "inconnu") + "'",
                "Employer", target != null ? target.getId() : null,
                target != null ? target.getName() : null);
    }

    /** Utilisateurs d'un tenant (support) — délégué à la liste de cibles. */
    public List<Employer> cibles(Long entrepriseId) {
        return employerRepository.findByEntrepriseId(entrepriseId);
    }
}