// ─── FeuilleTempsService.java (partie 1) ─────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.FeuilleTempsRequestDTO;
import com.example.backend_sirhgp.dto.response.FeuilleTempsResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.TimesheetMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.FeuilleTemps;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.FeuilleTempsRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeuilleTempsService {

    private final FeuilleTempsRepository feuilleRepository;
    private final EmployerRepository employerRepository;
    private final ProjetRepository projetRepository;
    private final TacheRepository tacheRepository;
    private final TimesheetMapper mapper;
    private final AuditLogService auditLogService;
    private final EntrepriseContextService entrepriseContextService;
    // Refonte LEAD 6.0 : périmètre départemental du chef d'équipe.
    private final LeadPerimetreService leadPerimetre;

    @Transactional
    public FeuilleTempsResponseDTO create(FeuilleTempsRequestDTO dto) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        Entreprise entreprise = current.getEntreprise();
        if (entreprise == null) {
            throw new IllegalArgumentException("Entreprise introuvable");
        }

        FeuilleTemps feuille = FeuilleTemps.builder()
                .date(dto.getDate())
                .heures(dto.getHeures())
                .description(dto.getDescription())
                .statut(FeuilleTemps.StatutFeuille.BROUILLON)
                .employe(current)
                .entreprise(entreprise)
                .build();

        if (dto.getProjetId() != null) {
            feuille.setProjet(projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId())));
        }
        if (dto.getTacheId() != null) {
            feuille.setTache(tacheRepository.findById(dto.getTacheId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + dto.getTacheId())));
        }

        FeuilleTemps saved = feuilleRepository.save(feuille);
        auditLogService.record(current, AuditActionType.CREATE,
                "Saisie de " + saved.getHeures() + "h le " + saved.getDate(),
                "FeuilleTemps", saved.getId(), null);
        log.info("Feuille de temps créée : {}h le {}", saved.getHeures(), saved.getDate());
        return mapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public FeuilleTempsResponseDTO findById(Long id) {
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        FeuilleTemps feuille = feuilleRepository.findByIdWithRelations(id);
        if (feuille == null) {
            throw new ResourceNotFoundException("Feuille de temps non trouvée : " + id);
        }
        if (scope != null && (feuille.getEntreprise() == null || !scope.equals(feuille.getEntreprise().getId()))) {
            throw new ResourceNotFoundException("Feuille de temps non trouvée : " + id);
        }
        // Refonte LEAD 6.0 : lecture d'une feuille par ID également bornée au
        // périmètre du chef de département.
        leadPerimetre.exigerEmployeDansLePerimetre(
                feuille.getEmploye() != null ? feuille.getEmploye() : null,
                "Consultation d'une feuille de temps");
        return mapper.toResponseDTO(feuille);
    }

    @Transactional(readOnly = true)
    public List<FeuilleTempsResponseDTO> findAll() {
        // Refonte LEAD 6.0 : périmètre départemental du chef d'équipe. Avant,
        // la liste était scopée à l'ENTREPRISE : un LEAD voyait les feuilles de
        // temps de tous ses collègues d'agence. Le filtre est appliqué sur
        // l'employé propriétaire de la feuille (les relations sont fetch-joinées
        // par le repository, donc aucun accès paresseux ici).
        if (leadPerimetre.estLead() && leadPerimetre.aPerimetreDepartemental()) {
            List<Long> employesIds = employerRepository
                    .findIdsByDepartementIdIn(leadPerimetre.departementsSupervises());
            if (employesIds.isEmpty()) return Collections.emptyList();
            return feuilleRepository.findByEmployeIdInWithRelations(employesIds).stream()
                    .map(mapper::toResponseDTO).collect(Collectors.toList());
        }
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<FeuilleTemps> list = scope != null
                ? feuilleRepository.findByEntrepriseIdWithRelations(scope)
                : feuilleRepository.findAllWithRelations();
        return list.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeuilleTempsResponseDTO> findByEmployeId(Long employeId) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current != null && !hasRoleGestion(current) && !current.getId().equals(employeId)) {
            return Collections.emptyList();
        }
        // Refonte LEAD 6.0 : un chef de département ne consulte que les feuilles
        // de ses employés (sinon il interrogeait n'importe quel ID d'entreprise).
        leadPerimetre.exigerEmployeDansLePerimetre(employeId, "Consultation des feuilles de temps");
        List<FeuilleTemps> list = feuilleRepository.findByEmployeIdWithRelations(employeId);
        return list.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeuilleTempsResponseDTO> findByStatut(FeuilleTemps.StatutFeuille statut) {
        // Refonte LEAD 6.0 : on réutilise le périmètre de findAll() puis on filtre
        // sur le statut — l'ancienne version interrogeait toute l'entreprise.
        if (leadPerimetre.estLead() && leadPerimetre.aPerimetreDepartemental()) {
            return findAll().stream()
                    .filter(f -> f.getStatut() == statut)
                    .collect(Collectors.toList());
        }
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<FeuilleTemps> list = scope != null
                ? feuilleRepository.findByEntrepriseIdAndStatutWithRelations(scope, statut)
                : feuilleRepository.findAllWithRelations();
        return list.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    /** Total d'heures d'un employé sur [debut, fin]. */
    @Transactional(readOnly = true)
    public BigDecimal totalHeures(Long employeId, LocalDate debut, LocalDate fin) {
        // Refonte LEAD 6.0 : un chef de département ne totalise que son équipe
        // (sinon il remontait les horaires de toute l'agence par simple ID).
        leadPerimetre.exigerPerimetreAncre(employeId, "Consultation du total d'heures");
        if (debut == null || fin == null) {
            debut = LocalDate.now().withDayOfMonth(1);
            fin = LocalDate.now();
        }
        return feuilleRepository.sumHeuresByEmployeAndPeriode(employeId, debut, fin);
    }

    /**
     * Workflow d'approbation.
     * PROPRIETAIRE peut soumettre sa feuille ; MANAGER+ peut approuver/rejeter.
     */
    @Transactional
    public FeuilleTempsResponseDTO updateStatut(Long id, FeuilleTemps.StatutFeuille statut) {
        FeuilleTemps feuille = feuilleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feuille non trouvée : " + id));
        Employer current = entrepriseContextService.getCurrentEmployer();

        boolean gestion = hasRoleGestion(current);
        boolean proprietaire = current != null && current.getId().equals(feuille.getEmploye().getId());

        // Refonte LEAD 6.0 : le chef de département approuve/rejette les feuilles
        // de SON équipe. Attention : on n'ajoute PAS « LEAD » à hasRoleGestion()
        // (utilisée ailleurs pour des droits d'entreprise entière) — on ouvre un
        // cas dédié, borné au périmètre, sinon il validerait les feuilles de
        // tous les départements de son agence.
        // On exige un ancrage départemental : sans département supervisé, le
        // repli de employeVisible() remonterait jusqu'à l'entreprise entière.
        Long employeFeuilleId = feuille.getEmploye() != null ? feuille.getEmploye().getId() : null;
        boolean chefEquipe = leadPerimetre.estLead()
                && leadPerimetre.aPerimetreDepartemental()
                && leadPerimetre.employeVisible(employeFeuilleId);

        if (statut == FeuilleTemps.StatutFeuille.APPROUVEE || statut == FeuilleTemps.StatutFeuille.REJETEE) {
            if (!gestion && !chefEquipe) {
                throw new AccessDeniedException(
                        "Seul un manager, ou le chef de département de l'intéressé, "
                                + "peut approuver ou rejeter une feuille de temps");
            }
        } else if (statut == FeuilleTemps.StatutFeuille.SOUMISE) {
            if (!proprietaire && !gestion) {
                throw new AccessDeniedException("Vous ne pouvez soumettre que vos propres feuilles");
            }
        } else { // BROUILLON (réouverture)
            if (!proprietaire && !gestion && !chefEquipe) {
                throw new AccessDeniedException("Action non autorisée sur cette feuille");
            }
        }

        feuille.setStatut(statut);
        FeuilleTemps saved = feuilleRepository.save(feuille);
        auditLogService.record(current, AuditActionType.UPDATE,
                "Feuille du " + saved.getDate() + " : " + statut,
                "FeuilleTemps", saved.getId(), null);
        return mapper.toResponseDTO(saved);
    }

    private boolean hasRoleGestion(Employer e) {
        if (e == null || e.getRole() == null) return false;
        String r = e.getRole().getName();
        return "SUPER_ADMIN".equals(r) || "ADMIN".equals(r) || "MANAGER".equals(r);
    }
}