package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.DemandePermissionRequestDTO;
import com.example.backend_sirhgp.dto.response.DemandePermissionResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.DemandePermission;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.repository.DemandePermissionRepository;
import com.example.backend_sirhgp.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Refonte 5.6 — Demandes de permission (Timesheet).
 *
 * Chaîne de validation par rôle :
 *   STAFF / STAGIAIRE → chef de département (superviseur LEAD, refonte 5.5)
 *   LEAD              → MANAGER de l'entreprise
 *   MANAGER           → ADMIN de l'entreprise
 *   ADMIN / SUPER_ADMIN → valideurId null (demande traitable par l'ADMIN du scope)
 *
 * Le valideur est résolu à la soumission puis SNAPSHOTTÉ : changer de chef de
 * département ne réécrit pas l'historique. Justificatif = référence d'un
 * Document existant (jamais d'auto-validation par le demandeur).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandePermissionService {

    private final DemandePermissionRepository permissionRepository;
    private final PermissionApproverResolver approverResolver;
    private final DocumentRepository documentRepository;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;

    // ─── Création ────────────────────────────────────────────────────────────

    @Transactional
    public DemandePermissionResponseDTO create(DemandePermissionRequestDTO dto) {
        Employer demandeur = entrepriseContextService.getCurrentEmployer();
        if (demandeur == null) throw new AccessDeniedException("Utilisateur non authentifié");
        Entreprise entreprise = demandeur.getEntreprise();

        validerPeriode(dto);

        long chevauchements = permissionRepository.countChevauchements(
                demandeur.getId(), dto.getDateDebut(), dto.getDateFin());
        if (chevauchements > 0) {
            throw new IllegalArgumentException(
                    "Une demande de permission existe déjà sur cette période (en attente ou approuvée)");
        }

        String justificatifNom = null;
        if (dto.getJustificatifId() != null) {
            Document doc = documentRepository.findById(dto.getJustificatifId())
                    .orElseThrow(() -> new ResourceNotFoundException("Justificatif non trouvé : " + dto.getJustificatifId()));
            if (doc.getExpediteur() == null || !doc.getExpediteur().getId().equals(demandeur.getId())) {
                throw new IllegalArgumentException("Le justificatif doit être un document que vous avez vous-même téléversé");
            }
            justificatifNom = doc.getNom();
        }

        Employer valideur = approverResolver.resolve(demandeur);

        DemandePermission perm = DemandePermission.builder()
                .type(dto.getType())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .heureDebut(dto.getHeureDebut())
                .heureFin(dto.getHeureFin())
                .motif(dto.getMotif().trim())
                .statut(DemandePermission.StatutPermission.EN_ATTENTE)
                .demandeur(demandeur)
                .valideurId(valideur != null ? valideur.getId() : null)
                .valideurNom(valideur != null ? valideur.getName() : null)
                .justificatifId(dto.getJustificatifId())
                .justificatifNom(justificatifNom)
                .entreprise(entreprise)
                .build();

        DemandePermission saved = permissionRepository.save(perm);
        auditLogService.record(demandeur, AuditActionType.CREATE,
                "Demande de permission " + saved.getType() + " du " + saved.getDateDebut()
                        + " au " + saved.getDateFin() + (valideur != null ? " → " + valideur.getName() : ""),
                "DemandePermission", saved.getId(), null);
        log.info("Demande de permission #{} créée par {} (valideur : {})",
                saved.getId(), demandeur.getEmail(), valideur != null ? valideur.getEmail() : "aucun (orpheline)");
        return toDTO(saved);
    }

    // ─── Consultation ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DemandePermissionResponseDTO> mesDemandes() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) throw new AccessDeniedException("Utilisateur non authentifié");
        return permissionRepository.findByDemandeurIdWithDemandeur(current.getId())
                .stream().map(this::toDTO).toList();
    }

    /**
     * File « À valider » : demandes où je suis le valideur snapshoté,
     * + demandes orphelines (valideurId null) pour MANAGER/ADMIN du scope.
     */
    @Transactional(readOnly = true)
    public List<DemandePermissionResponseDTO> aValider() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) throw new AccessDeniedException("Utilisateur non authentifié");

        List<DemandePermission> list = new java.util.ArrayList<>(
                permissionRepository.findEnAttenteByValideur(current.getId()));

        Long scope = entrepriseContextService.getScopeEntrepriseId();
        String role = entrepriseContextService.currentRole();
        boolean peutVoirOrphelines = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        if (peutVoirOrphelines) {
            list.addAll(scope != null
                    ? permissionRepository.findOrphelinesByEntreprise(scope)
                    : permissionRepository.findAllOrphelines());
        }
        return list.stream().map(this::toDTO).toList();
    }

    // ─── Décision (valideur) ─────────────────────────────────────────────────

    @Transactional
    public DemandePermissionResponseDTO decider(Long id, String statutStr, String commentaire) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) throw new AccessDeniedException("Utilisateur non authentifié");

        DemandePermission perm = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de permission non trouvée : " + id));

        if (!"EN_ATTENTE".equals(perm.getStatut().name())) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée (" + perm.getStatut() + ")");
        }
        if (!peutDecider(current, perm)) {
            throw new AccessDeniedException("Seul le valideur désigné (ou un administrateur de l'entreprise) peut traiter cette demande");
        }

        DemandePermission.StatutPermission statut;
        try {
            statut = DemandePermission.StatutPermission.valueOf(statutStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide : " + statutStr);
        }
        if (statut != DemandePermission.StatutPermission.APPROUVEE
                && statut != DemandePermission.StatutPermission.REJETEE) {
            throw new IllegalArgumentException("La décision doit être APPROUVEE ou REJETEE");
        }
        if (statut == DemandePermission.StatutPermission.REJETEE
                && (commentaire == null || commentaire.isBlank())) {
            throw new IllegalArgumentException("Un commentaire de rejet est obligatoire");
        }

        perm.setStatut(statut);
        perm.setCommentaireValideur(commentaire != null && !commentaire.isBlank() ? commentaire.trim() : null);
        perm.setDateDecision(java.time.LocalDateTime.now());
        DemandePermission saved = permissionRepository.save(perm);

        // demandeur est le valideur (auto-validation déjà interdite en amont).

        auditLogService.record(current, AuditActionType.UPDATE,
                "Demande de permission de " + saved.getDemandeur().getName() + " : " + statut,
                "DemandePermission", saved.getId(), null);
        log.info("Demande de permission #{} → {} par {}", saved.getId(), statut, current.getEmail());
        return toDTO(saved);
    }

    // ─── Annulation (demandeur) ──────────────────────────────────────────────

    @Transactional
    public DemandePermissionResponseDTO annuler(Long id) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) throw new AccessDeniedException("Utilisateur non authentifié");

        DemandePermission perm = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de permission non trouvée : " + id));

        if (!perm.getDemandeur().getId().equals(current.getId())) {
            throw new AccessDeniedException("Seul le demandeur peut annuler sa demande");
        }
        if (!"EN_ATTENTE".equals(perm.getStatut().name())) {
            throw new IllegalArgumentException("Seule une demande en attente peut être annulée");
        }

        perm.setStatut(DemandePermission.StatutPermission.ANNULEE);
        perm.setDateDecision(java.time.LocalDateTime.now());
        DemandePermission saved = permissionRepository.save(perm);

        auditLogService.record(current, AuditActionType.UPDATE,
                "Annulation de sa demande de permission du " + saved.getDateDebut(),
                "DemandePermission", saved.getId(), null);
        return toDTO(saved);
    }

    // ─── Résolution du valideur (chaîne par rôle, refonte 5.6) ───────────────

    /**
     * STAFF/STAGIAIRE → superviseur LEAD de son département (via poste) ;
     * LEAD → MANAGER de l'entreprise ; MANAGER → ADMIN ; ADMIN/SUPER_ADMIN → null.
     * Repli vers l'ADMIN de l'entreprise si le maillon est manquant,
     * et jamais l'employé lui-même (interdiction d'auto-validation).
     */
    private boolean peutDecider(Employer current, DemandePermission perm) {
        if (perm.getValideurId() != null && perm.getValideurId().equals(current.getId())) return true;
        String role = current.getRole() != null ? current.getRole().getName() : null;
        if ("SUPER_ADMIN".equals(role)) return true;
        if ("ADMIN".equals(role)) {
            Long scope = entrepriseContextService.getScopeEntrepriseId();
            return scope != null && perm.getEntreprise() != null && scope.equals(perm.getEntreprise().getId());
        }
        return false; // MANAGER, LEAD, STAFF, STAGIAIRE refusés
    }

    private void validerPeriode(DemandePermissionRequestDTO dto) {
        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
        if ("SORTIE".equals(dto.getType().name())) {
            if (!dto.getDateDebut().equals(dto.getDateFin())) {
                throw new IllegalArgumentException("Une SORTIE doit être sur une seule journée");
            }
            if (dto.getHeureDebut() == null || dto.getHeureFin() == null) {
                throw new IllegalArgumentException("Les heures de début et de fin sont obligatoires pour une SORTIE");
            }
            if (!dto.getHeureFin().isAfter(dto.getHeureDebut())) {
                throw new IllegalArgumentException("L'heure de fin doit être postérieure à l'heure de début");
            }
        }
    }

    private DemandePermissionResponseDTO toDTO(DemandePermission p) {
        DemandePermissionResponseDTO dto = new DemandePermissionResponseDTO();
        dto.setId(p.getId());
        dto.setType(p.getType());
        dto.setDateDebut(p.getDateDebut());
        dto.setDateFin(p.getDateFin());
        dto.setHeureDebut(p.getHeureDebut());
        dto.setHeureFin(p.getHeureFin());
        dto.setMotif(p.getMotif());
        dto.setStatut(p.getStatut());
        if (p.getDemandeur() != null) {
            dto.setDemandeurId(p.getDemandeur().getId());
            dto.setDemandeurNom(p.getDemandeur().getName());
        }
        dto.setValideurId(p.getValideurId());
        dto.setValideurNom(p.getValideurNom());
        dto.setCommentaireValideur(p.getCommentaireValideur());
        dto.setJustificatifId(p.getJustificatifId());
        dto.setJustificatifNom(p.getJustificatifNom());
        dto.setJustificatifUrl(p.getJustificatifId() != null
                ? "/api/documents/" + p.getJustificatifId() + "/download" : null);
        if (p.getEntreprise() != null) dto.setEntrepriseId(p.getEntreprise().getId());
        dto.setDateCreation(p.getDateCreation());
        dto.setDateDecision(p.getDateDecision());
        long jours = p.getType() == DemandePermission.TypePermission.SORTIE
                ? 1
                : ChronoUnit.DAYS.between(p.getDateDebut(), p.getDateFin()) + 1;
        dto.setDureeJours(jours);
        return dto;
    }
}
