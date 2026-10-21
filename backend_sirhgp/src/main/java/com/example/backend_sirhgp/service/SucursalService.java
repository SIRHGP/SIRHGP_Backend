// ─── SucursalService.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.SucursalRequestDTO;
import com.example.backend_sirhgp.dto.response.SucursalResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.SucursalMapper;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Sucursal;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.EntrepriseRepository;
import com.example.backend_sirhgp.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des sucursales (établissements secondaires, 3.4).
 * Scoping : ADMIN scopé à une sucursal -> la sienne ; ADMIN général -> son entreprise ; SUPER_ADMIN -> tout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EmployerRepository employerRepository;
    private final EntrepriseContextService entrepriseContextService;
    private final SucursalMapper sucursalMapper;
    private final AuditLogService auditLogService;
    private final QuotaService quotaService;

    private Long scopeEntreprise() {
        return entrepriseContextService.getScopeEntrepriseId();
    }

    private void checkScope(Long entrepriseId, String action) {
        Long scope = scopeEntreprise();
        if (scope == null && !entrepriseContextService.isSuperAdmin()) {
            // Tout compte non-SUPER_ADMIN doit être rattaché à une entreprise.
            throw new AccessDeniedException(
                    "Votre compte n'est rattaché à aucune entreprise (" + action + " refusée). "
                            + "Contactez le Super Admin.");
        }
        if (scope != null && !scope.equals(entrepriseId)) {
            throw new AccessDeniedException(
                    "Opération refusée : entreprise hors de votre périmètre (" + action + ")");
        }
    }

    @Transactional(readOnly = true)
    public List<SucursalResponseDTO> findAll(Long entrepriseId) {
        Long scope = scopeEntreprise();
        if (scope == null && !entrepriseContextService.isSuperAdmin()) {
            throw new AccessDeniedException(
                    "Votre compte n'est rattaché à aucune entreprise : liste refusée. Contactez le Super Admin.");
        }
        if (scope != null) entrepriseId = scope;
        if (entrepriseId == null) {
            return sucursalRepository.findAll().stream().map(sucursalMapper::toResponseDTO).toList();
        }
        getEntreprise(entrepriseId);
        List<Sucursal> list = sucursalRepository.findByEntrepriseIdOrderByNameAsc(entrepriseId);
        // 5.9 (§9) : un compte rattaché à une sucursal (MANAGER / LEAD) ne voit que la sienne.
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null) {
            final Long sc = scopeSucursal;
            list = list.stream().filter(s -> sc.equals(s.getId())).toList();
        }
        return list.stream().map(this::enrich).toList();
    }

    @Transactional(readOnly = true)
    public SucursalResponseDTO findById(Long id) {
        Sucursal s = getEntity(id);
        checkScope(s.getEntreprise().getId(), "lecture");
        return enrich(s);
    }

    @Transactional
    public SucursalResponseDTO create(Long entrepriseId, SucursalRequestDTO dto) {
        checkScope(entrepriseId, "création");
        Entreprise entreprise = getEntreprise(entrepriseId);

        String name = dto.getName().trim();
        if (sucursalRepository.existsByEntrepriseIdAndName(entrepriseId, name)) {
            throw new IllegalArgumentException("Cette sucursal existe déjà dans l'entreprise : " + name);
        }
        // 5.9 : quota plateforme (maxSucursales).
        Integer maxSuc = quotaService.maxSucursalesOf(entrepriseId);
        if (maxSuc != null && sucursalRepository.findByEntrepriseIdOrderByNameAsc(entrepriseId).size() >= maxSuc) {
            throw new IllegalArgumentException(
                    "Quota atteint : nombre maximum de succursales (" + maxSuc + ") pour cette entreprise (5.9)");
        }

        Sucursal s = Sucursal.builder()
                .name(name)
                .ville(dto.getVille())
                .adresse(dto.getAdresse())
                .telephone(dto.getTelephone())
                .statut(dto.getStatut() != null ? dto.getStatut() : Sucursal.StatutSucursal.ACTIVE)
                .entreprise(entreprise)
                .build();
        s = sucursalRepository.save(s);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Création de la sucursal '" + s.getName() + "' de l'entreprise '" + entreprise.getName() + "'",
                "Sucursal", s.getId(), s.getName());
        log.info("Sucursal créée : {} ({})", s.getName(), entrepriseId);
        return enrich(s);
    }

    @Transactional
    public SucursalResponseDTO update(Long id, SucursalRequestDTO dto) {
        Sucursal s = getEntity(id);
        checkScope(s.getEntreprise().getId(), "modification");

        if (dto.getName() != null && !dto.getName().isBlank()) {
            String name = dto.getName().trim();
            if (!name.equals(s.getName())
                    && sucursalRepository.existsByEntrepriseIdAndName(s.getEntreprise().getId(), name)) {
                throw new IllegalArgumentException("Sucursal déjà existante : " + name);
            }
            s.setName(name);
        }
        if (dto.getVille() != null) s.setVille(dto.getVille());
        if (dto.getAdresse() != null) s.setAdresse(dto.getAdresse());
        if (dto.getTelephone() != null) s.setTelephone(dto.getTelephone());
        if (dto.getStatut() != null) s.setStatut(dto.getStatut());

        s = sucursalRepository.save(s);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Modification de la sucursal '" + s.getName() + "'", "Sucursal", s.getId(), s.getName());
        return enrich(s);
    }

    @Transactional
    public SucursalResponseDTO toggleStatut(Long id) {
        Sucursal s = getEntity(id);
        checkScope(s.getEntreprise().getId(), "changement de statut");
        s.setStatut(s.getStatut() == Sucursal.StatutSucursal.ACTIVE
                ? Sucursal.StatutSucursal.SUSPENDUE
                : Sucursal.StatutSucursal.ACTIVE);
        s = sucursalRepository.save(s);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.TOGGLE_STATUS,
                "Statut de la sucursal '" + s.getName() + "' → " + s.getStatut(),
                "Sucursal", s.getId(), s.getName());
        return enrich(s);
    }

    @Transactional
    public void delete(Long id) {
        Sucursal s = getEntity(id);
        checkScope(s.getEntreprise().getId(), "suppression");

        long employes = employerRepository.countBySucursalId(id);
        if (employes > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer la sucursal '" + s.getName() + "' : "
                            + employes + " employé(s) y sont rattaché(s). Suspendez-la plutôt.");
        }
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Suppression de la sucursal '" + s.getName() + "'", "Sucursal", id, s.getName());
        sucursalRepository.delete(s);
        log.info("Sucursal supprimée : {}", id);
    }

    // ─── 5.9 (§7) — Affectation du manager de la sucursal (ADMIN seul) ────────

    /**
     * Affecte (managerId != null) ou désaffecte (managerId == null) le manager
     * d'une sucursal. Garde-fous : même entreprise, compte actif, jamais SUPER_ADMIN.
     */
    @Transactional
    public SucursalResponseDTO assignManager(Long id, Long managerId) {
        Sucursal s = getEntity(id);
        checkScope(s.getEntreprise().getId(), "affectation du manager");

        if (managerId == null) {
            s.setManager(null);
            s = sucursalRepository.save(s);
            auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.ASSIGN,
                    "Manager retiré de la sucursal '" + s.getName() + "'", "Sucursal", s.getId(), s.getName());
            return enrich(s);
        }

        Employer manager = employerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + managerId));
        if (manager.getEntreprise() == null
                || !s.getEntreprise().getId().equals(manager.getEntreprise().getId())) {
            throw new IllegalArgumentException("Le manager doit appartenir à la même entreprise que la sucursal");
        }
        if (!manager.isActif()) {
            throw new IllegalArgumentException("Le manager doit être un compte actif");
        }
        if (manager.getRole() != null && "SUPER_ADMIN".equals(manager.getRole().getName())) {
            throw new IllegalArgumentException("Un SUPER_ADMIN ne peut pas être manager de sucursal");
        }

        // Règle d'unicité : un employé ne peut être manager que d'UNE SEULE sucursale.
        final Long sucursalEnCoursId = s.getId();
        List<Sucursal> autres = sucursalRepository.findByManagerId(managerId);
        Sucursal deja = autres.stream().filter(x -> !x.getId().equals(sucursalEnCoursId)).findFirst().orElse(null);
        if (deja != null) {
            throw new IllegalArgumentException(
                    "'" + manager.getName() + "' est déjà manager de la sucursale '" + deja.getName()
                            + "'. Retirez-le d'abord de cette sucursale pour l'affecter ailleurs.");
        }

        s.setManager(manager);
        s = sucursalRepository.save(s);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.ASSIGN,
                "Manager '" + manager.getName() + "' affecté à la sucursal '" + s.getName() + "'",
                "Sucursal", s.getId(), s.getName());
        log.info("Sucursal {} : manager {} affecté", s.getId(), manager.getEmail());
        return enrich(s);
    }

    // ─── Utilitaires ─────────────────────────────────────────
    private Entreprise getEntreprise(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée : " + id));
    }

    private Sucursal getEntity(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal non trouvée : " + id));
    }

    private SucursalResponseDTO enrich(Sucursal s) {
        SucursalResponseDTO dto = sucursalMapper.toResponseDTO(s);
        dto.setNombreEmployes(employerRepository.countBySucursalId(s.getId()));
        return dto;
    }
}