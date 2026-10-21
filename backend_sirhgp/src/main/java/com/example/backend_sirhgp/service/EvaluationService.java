// ─── EvaluationService.java ────────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.CritereEvaluationDTO;
import com.example.backend_sirhgp.dto.request.EvaluationRequestDTO;
import com.example.backend_sirhgp.dto.response.EvaluationKpiDTO;
import com.example.backend_sirhgp.dto.response.EvaluationResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.EvaluationMapper;
import com.example.backend_sirhgp.model.CritereEvaluation;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Evaluation;
import com.example.backend_sirhgp.model.Pointage;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.EvaluationRepository;
import com.example.backend_sirhgp.repository.PointageRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module Évaluations (2.4 — refonte 6.1).
 *
 * <p>Règles verrouillées par cette classe (au-delà du matcher SecurityConfig) :
 * <ul>
 *   <li><b>Évaluateur forcé</b> = utilisateur courant (jamais choisi par le client) ;</li>
 *   <li><b>Hiérarchie des rangs</b> : on n'évalue qu'un rang strictement inférieur
 *       au sien — LEAD → STAFF/STAGIAIRE de SES départements ; MANAGER → LEAD et
 *       en dessous ; ADMIN → MANAGER et en dessous ;</li>
 *   <li><b>Lecture bornée</b> : LEAD = département supervisé ; MANAGER = évaluations
 *       dont l'évaluateur est un LEAD (à valider) ou dont il est l'auteur/évalué ;
 *       ADMIN = entreprise ; STAFF/STAGIAIRE = refusés (403) ;</li>
 *   <li><b>Validation</b> : réservée à une évaluation SOUMISE écrite par un LEAD,
 *       par un MANAGER (ou ADMIN en repli) de l'entreprise ;</li>
 *   <li><b>Rapport</b> figé à la validation (récap KPI + critères + note + décision).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EmployerRepository employerRepository;
    private final TacheRepository tacheRepository;
    private final PointageRepository pointageRepository;
    private final EvaluationMapper mapper;
    private final AuditLogService auditLogService;
    private final EntrepriseContextService entrepriseContextService;
    private final LeadPerimetreService leadPerimetre;

    @Transactional
    public EvaluationResponseDTO create(EvaluationRequestDTO dto) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        String roleCreateur = roleName(current);
        if (!estEvaluateurAutorise(roleCreateur)) {
            throw new AccessDeniedException("Votre rôle ne permet pas de créer une évaluation");
        }
        Entreprise entreprise = current.getEntreprise();
        if (entreprise == null) {
            throw new IllegalArgumentException("Entreprise introuvable");
        }

        Employer evalue = employerRepository.findById(dto.getEvalueId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé évalué introuvable : " + dto.getEvalueId()));

        if (evalue.getEntreprise() != null && !evalue.getEntreprise().getId().equals(entreprise.getId())) {
            throw new AccessDeniedException("L'employé évalué n'appartient pas à votre entreprise");
        }

        // Hiérarchie des rangs : uniquement un rang strictement inférieur au sien.
        verifierRangEvaluateur(roleCreateur, roleName(evalue));
        // Refonte LEAD 6.0/6.1 : le LEAD n'évalue que SON équipe, ancrage requis.
        leadPerimetre.exigerPerimetreAncre(evalue.getId(), "Création d'une évaluation");

        // Évaluateur = l'utilisateur courant, toujours (le DTO ne porte pas d'info fiable).
        Evaluation.StatutEvaluation statutInitial = "LEAD".equals(roleCreateur)
                ? Evaluation.StatutEvaluation.SOUMISE   // soumis au MANAGER dès la création
                : Evaluation.StatutEvaluation.VALIDEE;  // MANAGER/ADMIN : pas de validation externe

        Evaluation evaluation = Evaluation.builder()
                .type(dto.getType())
                .categorie(dto.getCategorie())
                .objet(dto.getObjet() != null ? dto.getObjet().trim() : null)
                .annee(dto.getAnnee() != null ? dto.getAnnee() : java.time.LocalDate.now().getYear())
                .periode(dto.getPeriode())
                .statut(statutInitial)
                .dateEvaluation(dto.getDateEvaluation())
                .commentaire(dto.getCommentaire())
                .evalue(evalue)
                .evaluateur(current)
                .entreprise(entreprise)
                .build();

        if (dto.getCriteres() != null) {
            for (CritereEvaluationDTO cDto : dto.getCriteres()) {
                validerCoefficient(cDto.getCoefficient());
                evaluation.addCritere(mapper.toEntity(cDto, evaluation));
            }
            evaluation.calculerMoyenne();
        }

        // Évaluations d'un MANAGER/ADMIN : validées d'office → rapport figé immédiatement.
        if (statutInitial == Evaluation.StatutEvaluation.VALIDEE) {
            figerRapport(evaluation);
        }

        Evaluation saved = evaluationRepository.save(evaluation);
        auditLogService.record(current, AuditActionType.CREATE,
                "Évaluation " + saved.getCategorie() + " de " + saved.getEvalue().getName()
                        + " (" + saved.getStatut() + ")",
                "Evaluation", saved.getId(), null);
        log.info("Évaluation créée : {} / {} de {} par {} ({})",
                saved.getCategorie(), saved.getType(), saved.getEvalue().getName(),
                current.getName(), roleCreateur);
        EvaluationResponseDTO response =
                mapper.toResponseDTO(evaluationRepository.findByIdWithRelations(saved.getId()));
        enrichirValideur(response, current, entreprise);
        return response;
    }
    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> findAll() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = roleName(current);
        exigerAccesRH(role);

        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<Evaluation> list = scope != null
                ? evaluationRepository.findByEntrepriseIdWithRelations(scope)
                : evaluationRepository.findAllWithRelations();

        // LEAD : seul son périmètre départemental (sans ancrage → liste vide,
        // convention 6.0 : une équipe vide ne dévoile pas toute l'entreprise).
        if ("LEAD".equals(role)) {
            if (!leadPerimetre.aPerimetreDepartemental()) return Collections.emptyList();
            List<Long> evalueIds = employerRepository
                    .findIdsByDepartementIdIn(leadPerimetre.departementsSupervises());
            if (evalueIds.isEmpty()) return Collections.emptyList();
            return list.stream()
                    .filter(ev -> ev.getEvalue() != null && evalueIds.contains(ev.getEvalue().getId()))
                    .map(mapper::toResponseDTO).collect(Collectors.toList());
        }

        // MANAGER : celles à valider (auteur = LEAD) + celles dont il est l'auteur/évalué.
        if ("MANAGER".equals(role)) {
            return list.stream()
                    .filter(ev -> "LEAD".equals(roleName(ev.getEvaluateur()))
                            || memeId(ev.getEvaluateur(), current)
                            || memeId(ev.getEvalue(), current))
                    .map(mapper::toResponseDTO).collect(Collectors.toList());
        }

        return list.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> findByEvalue(Long evalueId) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        exigerAccesRH(roleName(current));
        // LEAD : ne consulte que les évaluations de SON équipe.
        leadPerimetre.exigerEmployeDansLePerimetre(evalueId, "Consultation des évaluations");
        return evaluationRepository.findByEvalueIdWithRelations(evalueId)
                .stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponseDTO> findByEvaluateur(Long evaluateurId) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = roleName(current);
        exigerAccesRH(role);
        // Un LEAD ne sonde pas les évaluations d'un autre évaluateur hors périmètre.
        if ("LEAD".equals(role) && !current.getId().equals(evaluateurId)) {
            throw new AccessDeniedException("Consultation des évaluations : évaluateur hors de votre périmètre");
        }
        return evaluationRepository.findByEvaluateurIdWithRelations(evaluateurId)
                .stream().map(mapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvaluationResponseDTO findById(Long id) {
        Evaluation evaluation = chargerEtControler(id, "Consultation d'une évaluation");
        EvaluationResponseDTO dto = mapper.toResponseDTO(evaluation);
        enrichirValideur(dto, evaluation.getEvaluateur(), evaluation.getEntreprise());
        return dto;
    }

    /**
     * Workflow d'états (6.1) :
     * <ul>
     *   <li>SOUMISE : réservée à l'évaluateur lui-même (LEAD) ;</li>
     *   <li>VALIDEE : réservée à un MANAGER (ADMIN en repli) d'entreprise, sur une
     *       évaluation SOUMISE dont l'auteur est un LEAD — génère le rapport.</li>
     * </ul>
     */
    @Transactional
    public EvaluationResponseDTO updateStatut(Long id, Evaluation.StatutEvaluation statut) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable : " + id));
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = roleName(current);
        exigerAccesRH(role);

        String roleEvaluateur = roleName(evaluation.getEvaluateur());

        if (statut == Evaluation.StatutEvaluation.VALIDEE) {
            if (!"MANAGER".equals(role) && !"ADMIN".equals(role)) {
                throw new AccessDeniedException("Seul un manager peut valider une évaluation");
            }
            // Seules les évaluations écrites par un LEAD exigent une validation.
            if (!"LEAD".equals(roleEvaluateur)) {
                throw new AccessDeniedException(
                        "Cette évaluation n'a pas besoin de validation : créée par un "
                                + roleEvaluateur + ", elle est déjà validée");
            }
            if (evaluation.getStatut() != Evaluation.StatutEvaluation.SOUMISE) {
                throw new AccessDeniedException("Seule une évaluation soumise peut être validée");
            }
            verifierScopeEntreprise(evaluation);
            if ("MANAGER".equals(role)) verifierScopeSucursale(evaluation, current);
            evaluation.setStatut(Evaluation.StatutEvaluation.VALIDEE);
            figerRapport(evaluation);
        } else if (statut == Evaluation.StatutEvaluation.SOUMISE) {
            if (!memeId(current, evaluation.getEvaluateur())) {
                throw new AccessDeniedException("Seul l'évaluateur peut soumettre cette évaluation");
            }
            if (evaluation.getStatut() != Evaluation.StatutEvaluation.BROUILLON) {
                throw new AccessDeniedException("Seule une évaluation en brouillon peut être soumise");
            }
            evaluation.setStatut(Evaluation.StatutEvaluation.SOUMISE);
        } else {
            throw new IllegalArgumentException("Transition de statut non supportée : " + statut);
        }

        Evaluation saved = evaluationRepository.save(evaluation);
        auditLogService.record(current, AuditActionType.UPDATE,
                "Évaluation de " + evaluation.getEvalue().getName() + " : " + statut,
                "Evaluation", saved.getId(), null);
        EvaluationResponseDTO response =
                mapper.toResponseDTO(evaluationRepository.findByIdWithRelations(saved.getId()));
        enrichirValideur(response, evaluation.getEvaluateur(), evaluation.getEntreprise());
        return response;
    }

    // ─── KPI (6.1) ─────────────────────────────────────────────────────────────

    /**
     * KPI de l'évalué pour alimenter le formulaire et le rapport :
     * tâches (global) + assiduité des 30 derniers jours (pointages / retards).
     */
    @Transactional(readOnly = true)
    public EvaluationKpiDTO kpi(Long employeId) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        exigerAccesRH(roleName(current));
        Employer employe = employerRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + employeId));
        // Même garde de périmètre que la création : pas de KPI hors équipe pour un LEAD.
        leadPerimetre.exigerEmployeDansLePerimetre(employeId, "Consultation des KPI d'évaluation");
        if (employe.getEntreprise() != null) {
            Long scope = entrepriseContextService.getScopeEntrepriseId();
            if (scope != null && !scope.equals(employe.getEntreprise().getId())) {
                throw new AccessDeniedException("Employé hors de votre entreprise");
            }
        }
        return calculerKpi(employe);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Tâches + assiduité (30 derniers jours) de l'évalué. */
    private EvaluationKpiDTO calculerKpi(Employer employe) {
        EvaluationKpiDTO kpi = new EvaluationKpiDTO();
        Long id = employe.getId();
        kpi.setEmployeId(id);
        kpi.setEmployeNom(employe.getName());

        long total = tacheRepository.countByAssigneeId(id);
        long terminees = tacheRepository.countByAssigneeIdAndStatut(id, Tache.StatutTache.TERMINEE);
        long enCours = tacheRepository.countByAssigneeIdAndStatut(id, Tache.StatutTache.EN_COURS);
        long enRetard = tacheRepository.countTachesEnRetardByAssigneeId(
                id, LocalDate.now(), Tache.StatutTache.TERMINEE);
        kpi.setTachesTotal(total);
        kpi.setTachesTerminees(terminees);
        kpi.setTachesEnCours(enCours);
        kpi.setTachesEnRetard(enRetard);
        kpi.setTauxCompletion(total > 0 ? Math.round(terminees * 100.0 / total) : 0L);

        // Assiduité : 30 derniers jours vs pointages (lun→ven).
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(29);
        List<Pointage> pointages =
                pointageRepository.findByEmployeIdAndDateBetweenOrderByDateDesc(id, debut, fin);
        long joursOuvres = 0;
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() <= 5) joursOuvres++;
        }
        LocalTime heureStandard = standardArrivee(employe);
        long retards = pointages.stream().filter(p -> heureStandard != null
                && p.getArrivee() != null
                && p.getArrivee().toLocalTime().isAfter(heureStandard)).count();
        kpi.setJoursOuvres(joursOuvres);
        kpi.setJoursPointes((long) pointages.size());
        kpi.setRetards(retards);
        kpi.setAbsences((long) Math.max(0, joursOuvres - pointages.size()));
        return kpi;
    }

    private LocalTime standardArrivee(Employer employe) {
        Entreprise entreprise = employe.getEntreprise();
        if (entreprise != null && entreprise.getHeureArriveeStandard() != null) {
            return entreprise.getHeureArriveeStandard();
        }
        return LocalTime.of(8, 0);
    }

    /** Rapport figé (6.1) : récap automatique KPI + critères + note + décision. */
    private void figerRapport(Evaluation e) {
        EvaluationKpiDTO kpi = calculerKpi(e.getEvalue());
        StringBuilder sb = new StringBuilder();
        sb.append("Rapport d'évaluation du ").append(LocalDate.now()).append('\n');
        sb.append("Objet : ").append(nvl(e.getObjet())).append('\n');
        sb.append("Catégorie : ").append(e.getCategorie())
          .append(" · Type : ").append(e.getType()).append('\n');
        sb.append("Interpellés : ")
          .append(nomEtRole(e.getEvaluateur())).append(" (évaluateur) → ")
          .append(nomEtRole(e.getEvalue())).append(" (évalué)").append('\n');
        sb.append("\n— Tâches (global) —\n");
        sb.append("Total : ").append(kpi.getTachesTotal())
          .append(" · Terminées : ").append(kpi.getTachesTerminees())
          .append(" · En cours : ").append(kpi.getTachesEnCours())
          .append(" · En retard : ").append(kpi.getTachesEnRetard())
          .append(" · Taux : ").append(kpi.getTauxCompletion()).append(" %\n");
        sb.append("Assiduité (30 j) : ").append(kpi.getJoursPointes()).append('/')
          .append(kpi.getJoursOuvres()).append(" jours pointés · ")
          .append(kpi.getRetards()).append(" retard(s) · ")
          .append(kpi.getAbsences()).append(" absence(s)\n");
        sb.append("\n— Critères —\n");
        if (e.getCriteres() == null || e.getCriteres().isEmpty()) {
            sb.append("Aucun critère renseigné.\n");
        } else {
            for (CritereEvaluation c : e.getCriteres()) {
                sb.append("· ").append(c.getLibelle())
                  .append(" : ").append(c.getNote()).append("/20 × coef ")
                  .append(c.getCoefficient()).append('\n');
            }
        }
        BigDecimal note = e.getNoteGlobale() != null ? e.getNoteGlobale() : e.calculerMoyenne();
        sb.append("Note globale : ")
          .append(note != null ? note.toPlainString() + "/20" : "—").append('\n');
        sb.append("\n— Décision —\n");
        sb.append("Catégorie retenue : ").append(e.getCategorie()).append('\n');
        if (e.getCommentaire() != null && !e.getCommentaire().isBlank()) {
            sb.append("Commentaire : ").append(e.getCommentaire().trim()).append('\n');
        }
        e.setRapport(sb.toString());
        e.setDateRapport(LocalDate.now());
    }

    /** Charge l'évaluation et applique les gardes de lecture (LEAD / MANAGER). */
    private Evaluation chargerEtControler(Long id, String action) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = roleName(current);
        exigerAccesRH(role);
        Evaluation evaluation = evaluationRepository.findByIdWithRelations(id);
        if (evaluation == null) {
            throw new ResourceNotFoundException("Évaluation introuvable : " + id);
        }
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        if (scope != null && evaluation.getEntreprise() != null
                && !scope.equals(evaluation.getEntreprise().getId())) {
            throw new ResourceNotFoundException("Évaluation introuvable : " + id);
        }
        if ("LEAD".equals(role)) {
            leadPerimetre.exigerEmployeDansLePerimetre(evaluation.getEvalue(), action);
        } else if ("MANAGER".equals(role)) {
            boolean pertinent = "LEAD".equals(roleName(evaluation.getEvaluateur()))
                    || memeId(evaluation.getEvaluateur(), current)
                    || memeId(evaluation.getEvalue(), current);
            if (!pertinent) {
                throw new AccessDeniedException(action + " : cette évaluation n'est pas dans votre périmètre");
            }
        }
        return evaluation;
    }

    /** STAFF / STAGIAIRE : aucun accès au module (refonte 6.1). */
    private void exigerAccesRH(String role) {
        if (role == null || "STAFF".equals(role) || "STAGIAIRE".equals(role)) {
            throw new AccessDeniedException("Les employés n'ont pas accès aux évaluations");
        }
    }

    /** Rôles pouvant créer une évaluation. */
    private boolean estEvaluateurAutorise(String role) {
        return "LEAD".equals(role) || "MANAGER".equals(role) || "ADMIN".equals(role);
    }

    /**
     * Hiérarchie des rangs : STAGIAIRE &lt; STAFF &lt; LEAD &lt; MANAGER &lt; ADMIN.
     * On n'évalue qu'un rang strictement inférieur au sien (LEAD → STAFF/STAGIAIRE,
     * MANAGER → LEAD et en dessous, ADMIN → MANAGER et en dessous).
     */
    private void verifierRangEvaluateur(String roleCreateur, String roleCible) {
        int rangCreateur = rang(roleCreateur);
        int rangCible = rang(roleCible);
        if (rangCible <= 0 || rangCreateur <= 0) {
            throw new AccessDeniedException("Rôle évalué non géré : " + roleCible);
        }
        if (rangCible >= rangCreateur) {
            throw new AccessDeniedException(
                    "Un " + roleCreateur + " ne peut pas évaluer un " + roleCible);
        }
    }

    private int rang(String role) {
        if (role == null) return 0;
        return switch (role) {
            case "STAGIAIRE" -> 1;
            case "STAFF" -> 2;
            case "LEAD" -> 3;
            case "MANAGER" -> 4;
            case "ADMIN" -> 5;
            default -> 0;
        };
    }

    private void verifierScopeEntreprise(Evaluation evaluation) {
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        if (scope != null && evaluation.getEntreprise() != null
                && !scope.equals(evaluation.getEntreprise().getId())) {
            throw new AccessDeniedException("Évaluation hors de votre entreprise");
        }
    }

    /** MANAGER scopé succursale : le LEAD auteur doit être de sa succursale. */
    private void verifierScopeSucursale(Evaluation evaluation, Employer current) {
        Long scope = entrepriseContextService.getScopeSucursalId();
        if (scope == null) return;
        Employer evaluateur = evaluation.getEvaluateur();
        if (evaluateur != null && evaluateur.getSucursal() != null
                && !scope.equals(evaluateur.getSucursal().getId())) {
            throw new AccessDeniedException("Évaluation hors de votre succursale");
        }
    }

    /**
     * Renseigne le MANAGER destinataire de la validation (cible de la notification
     * LEAD → MANAGER). Chaîne identique aux permissions (5.6) : LEAD → MANAGER de
     * l'entreprise, repli ADMIN, jamais l'auteur lui-même.
     */
    private void enrichirValideur(EvaluationResponseDTO dto, Employer auteur, Entreprise entreprise) {
        if (dto == null || auteur == null || !"LEAD".equals(roleName(auteur))) return;
        Long entrepriseId = entreprise != null ? entreprise.getId() : null;
        List<Employer> managers = entrepriseId != null
                ? employerRepository.findManagersActifsByEntrepriseId(entrepriseId)
                : employerRepository.findAllManagersActifs();
        Employer valideur = managers == null ? null : managers.stream()
                .filter(m -> !memeId(m, auteur))
                .findFirst().orElse(null);
        if (valideur == null && entrepriseId != null) {
            valideur = employerRepository.findAdminsByEntrepriseId(entrepriseId).stream()
                    .filter(a -> !memeId(a, auteur))
                    .findFirst().orElse(null);
        }
        if (valideur != null) {
            dto.setValideurId(valideur.getId());
            dto.setValideurNom(valideur.getName());
        }
    }

    private String nomEtRole(Employer e) {
        if (e == null) return "—";
        String role = roleName(e);
        return e.getName() + (role != null ? " (" + role + ")" : "");
    }

    private String roleName(Employer e) {
        if (e == null) return null;
        Role role = e.getRole();
        return role != null ? role.getName() : null;
    }

    private boolean memeId(Employer a, Employer b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    private String nvl(String v) {
        return v == null || v.isBlank() ? "—" : v.trim();
    }

    /**
     * Valide le coefficient avant insertion en base.
     * @param coefficient valeur reçue (peut être null → par défaut 1)
     */
    private void validerCoefficient(BigDecimal coefficient) {
        if (coefficient == null) {
            return; // le mapper appliquera le défaut 1
        }
        if (coefficient.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Coefficient invalide : doit être >= 0");
        }
        if (coefficient.compareTo(new BigDecimal("99.99")) > 0) {
            throw new IllegalArgumentException(
                    "Coefficient invalide : doit être <= 99.99 (limite DECIMAL(3,2) de la colonne criteres_evaluation)");
        }
    }
}