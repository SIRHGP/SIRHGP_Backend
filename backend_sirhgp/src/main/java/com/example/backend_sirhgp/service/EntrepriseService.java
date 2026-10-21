// ─── EntrepriseService.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.dashboard.SuperAdminStatsDTO;
import com.example.backend_sirhgp.dto.request.EntrepriseRequestDTO;
import com.example.backend_sirhgp.dto.request.PointageConfigDTO;
import com.example.backend_sirhgp.dto.request.AdminCreateRequestDTO;
import com.example.backend_sirhgp.dto.response.ResetPasswordResponseDTO;
import com.example.backend_sirhgp.dto.response.EntrepriseResponseDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.EmployerMapper;
import com.example.backend_sirhgp.mapper.EntrepriseMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.EntrepriseRepository;
import com.example.backend_sirhgp.repository.FactureRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import com.example.backend_sirhgp.repository.RoleRepository;
import com.example.backend_sirhgp.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des entreprises (multi-tenant).
 *
 * Règles d'accès (déclarées aussi côté contrôleur) :
 * - SUPER_ADMIN : CRUD complet sur toutes les entreprises ;
 * - ADMIN : consultation de sa propre entreprise uniquement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final EmployerRepository employerRepository;
    private final ProjetRepository projetRepository;
    private final TacheRepository tacheRepository;
    private final FactureRepository factureRepository;
    private final EntrepriseMapper entrepriseMapper;
    private final EmployerMapper employerMapper;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EntrepriseResponseDTO create(EntrepriseRequestDTO dto) {
        String name = dto.getName().trim();
        if (entrepriseRepository.existsByName(name)) {
            throw new IllegalArgumentException("Cette entreprise existe déjà : " + name);
        }

        Entreprise entreprise = Entreprise.builder()
                .name(name)
                .description(dto.getDescription())
                .secteurActivite(dto.getSecteurActivite())
                .type(dto.getType() != null ? dto.getType() : Entreprise.TypeEntreprise.AUTRE)
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .telephone(dto.getTelephone())
                .email(dto.getEmail())
                .logoUrl(dto.getLogoUrl())
                .statut(dto.getStatut() != null ? dto.getStatut() : Entreprise.StatutEntreprise.ACTIVE)
                .build();

        entreprise = entrepriseRepository.save(entreprise);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Création de l'entreprise '" + entreprise.getName() + "'",
                "Entreprise", entreprise.getId(), entreprise.getName());

        log.info("Entreprise créée : {}", entreprise.getName());
        return enrichAndMap(entreprise);
    }

    @Transactional(readOnly = true)
    public Page<EntrepriseResponseDTO> findAll(Pageable pageable) {
        // Multi-tenant : seul le SUPER_ADMIN voit toutes les entreprises ;
        // un ADMIN est limité à la sienne (dont les listes déroulantes sucursales/admins).
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        if (scope == null) {
            return entrepriseRepository.findAll(pageable).map(this::enrichAndMap);
        }
        return entrepriseRepository.findById(scope)
                .map(e -> new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(enrichAndMap(e)), pageable, 1))
                .orElse(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(), pageable, 0));
    }

    @Transactional(readOnly = true)
    public EntrepriseResponseDTO findById(Long id) {
        Entreprise entreprise = getEntity(id);
        // Multi-tenant : un ADMIN ne doit lire que sa propre entreprise (le SUPER_ADMIN voit tout).
        checkAdminScope(entreprise.getId());
        return enrichAndMap(entreprise);
    }

    @Transactional(readOnly = true)
    public Entreprise getEntity(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée : " + id));
    }

    @Transactional
    public EntrepriseResponseDTO update(Long id, EntrepriseRequestDTO dto) {
        Entreprise entreprise = getEntity(id);

        if (dto.getName() != null && !dto.getName().isBlank()) {
            String newName = dto.getName().trim();
            if (!newName.equals(entreprise.getName()) && entrepriseRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Nom d'entreprise déjà utilisé : " + newName);
            }
            entreprise.setName(newName);
        }
        if (dto.getDescription() != null) entreprise.setDescription(dto.getDescription());
        if (dto.getSecteurActivite() != null) entreprise.setSecteurActivite(dto.getSecteurActivite());
        if (dto.getType() != null) entreprise.setType(dto.getType());
        if (dto.getAdresse() != null) entreprise.setAdresse(dto.getAdresse());
        if (dto.getVille() != null) entreprise.setVille(dto.getVille());
        if (dto.getTelephone() != null) entreprise.setTelephone(dto.getTelephone());
        if (dto.getEmail() != null) entreprise.setEmail(dto.getEmail());
        if (dto.getLogoUrl() != null) entreprise.setLogoUrl(dto.getLogoUrl());
        if (dto.getStatut() != null) entreprise.setStatut(dto.getStatut());

        entreprise = entrepriseRepository.save(entreprise);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Modification de l'entreprise '" + entreprise.getName() + "'",
                "Entreprise", entreprise.getId(), entreprise.getName());

        return enrichAndMap(entreprise);
    }

    // ─── Admins d'une entreprise (Super Admin, 3.2) ──────────────

    /**
     * Liste des comptes ADMIN d'une entreprise.
     * Réservé au SUPER_ADMIN (côté contrôleur).
     */
    @Transactional(readOnly = true)
    public List<com.example.backend_sirhgp.dto.response.EmployerResponseDTO> getAdmins(Long entrepriseId) {
        getEntity(entrepriseId); // 404 si entreprise inexistante
        return employerRepository.findAdminsByEntrepriseId(entrepriseId).stream()
                .map(employerMapper::toResponseDTO)
                .toList();
    }

    /**
     * Active / désactive un admin d'une entreprise.
     * Règle de sécurité : impossible de désactiver le dernier admin actif
     * (une entreprise ne doit jamais rester sans admin).
     */
    @Transactional
    public com.example.backend_sirhgp.dto.response.EmployerResponseDTO toggleAdminActif(Long entrepriseId, Long adminId) {
        getEntity(entrepriseId);
        Employer admin = employerRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé : " + adminId));

        if (admin.getEntreprise() == null || !entrepriseId.equals(admin.getEntreprise().getId())) {
            throw new IllegalArgumentException("Ce compte n'appartient pas à cette entreprise");
        }
        if (!"ADMIN".equalsIgnoreCase(admin.getRole() != null ? admin.getRole().getName() : "")) {
            throw new IllegalArgumentException("Ce compte n'est pas un administrateur");
        }
        if (admin.isActif() && employerRepository.countActiveAdminsByEntrepriseId(entrepriseId) <= 1) {
            throw new IllegalArgumentException(
                    "Impossible de désactiver le dernier admin actif de cette entreprise. Créez d'abord un autre admin.");
        }

        admin.setActif(!admin.isActif());
        Employer saved = employerRepository.save(admin);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                (saved.isActif() ? "Activation" : "Désactivation") + " de l'admin '" + saved.getName()
                        + "' (entreprise '" + saved.getEntreprise().getName() + "')",
                "Employer", saved.getId(), saved.getName());

        log.info("Admin {} : {} ({})", saved.getEmail(), saved.isActif() ? "activé" : "désactivé", entrepriseId);
        return employerMapper.toResponseDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Entreprise entreprise = getEntity(id);

        long nbEmployes = employerRepository.countByEntrepriseId(id);
        if (nbEmployes > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer l'entreprise '" + entreprise.getName()
                            + "' : " + nbEmployes + " employé(s) y sont rattaché(s). Suspendez-la plutôt.");
        }

        entrepriseRepository.delete(entreprise);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Suppression de l'entreprise '" + entreprise.getName() + "'",
                "Entreprise", id, entreprise.getName());

        log.info("Entreprise supprimée : {}", id);
    }

    /**
     * Statistiques transverses du Super Admin (toutes entreprises confondues).
     */
    @Transactional(readOnly = true)
    public SuperAdminStatsDTO getSuperAdminStats() {
        List<Entreprise> entreprises = entrepriseRepository.findAll();

        // Agrégations en une requête GROUP BY par entité (évite le N+1)
        Map<Long, Long> employesParEntreprise = toCountMap(employerRepository.countGroupedByEntreprise());
        Map<Long, Long> projetsParEntreprise = toCountMap(projetRepository.countGroupedByEntreprise());
        Map<Long, Long> tachesParEntreprise = toCountMap(tacheRepository.countGroupedByEntreprise());
        Map<Long, Long> facturesParEntreprise = toCountMap(factureRepository.countGroupedByEntreprise());
        Map<Long, BigDecimal> montantsParEntreprise = toSumMap(factureRepository.sumMontantTTCGroupedByEntreprise());

        long totalEmployes = 0, totalProjets = 0, totalTaches = 0, totalFactures = 0;
        BigDecimal montantTotal = BigDecimal.ZERO;
        long actives = 0, suspendues = 0;

        List<SuperAdminStatsDTO.EntrepriseStatsDTO> parEntreprise = new ArrayList<>();

        for (Entreprise e : entreprises) {
            if (e.getStatut() == Entreprise.StatutEntreprise.ACTIVE) actives++;
            else suspendues++;

            long nbEmployes = employesParEntreprise.getOrDefault(e.getId(), 0L);
            long nbProjets = projetsParEntreprise.getOrDefault(e.getId(), 0L);
            long nbTaches = tachesParEntreprise.getOrDefault(e.getId(), 0L);
            long nbFactures = facturesParEntreprise.getOrDefault(e.getId(), 0L);
            BigDecimal montant = montantsParEntreprise.get(e.getId());

            totalEmployes += nbEmployes;
            totalProjets += nbProjets;
            totalTaches += nbTaches;
            totalFactures += nbFactures;
            montantTotal = montantTotal.add(montant != null ? montant : BigDecimal.ZERO);

            parEntreprise.add(SuperAdminStatsDTO.EntrepriseStatsDTO.builder()
                    .entrepriseId(e.getId())
                    .nom(e.getName())
                    .statut(e.getStatut() != null ? e.getStatut().name() : null)
                    .nbEmployes(nbEmployes)
                    .nbProjets(nbProjets)
                    .nbTaches(nbTaches)
                    .nbFactures(nbFactures)
                    .montantFactures(montant)
                    .build());
        }

        return SuperAdminStatsDTO.builder()
                .totalEntreprises(entreprises.size())
                .entreprisesActives(actives)
                .entreprisesSuspendues(suspendues)
                .totalEmployes(totalEmployes)
                .totalProjets(totalProjets)
                .totalTaches(totalTaches)
                .totalFactures(totalFactures)
                .montantTotalFactures(montantTotal)
                .parEntreprise(parEntreprise)
                .build();
    }

    @Transactional
    public EntrepriseResponseDTO updatePointageConfig(Long id, PointageConfigDTO dto) {
        // Scoping : l'ADMIN ne configure que sa propre entreprise ; le SUPER_ADMIN voit tout.
        checkAdminScope(id);
        Entreprise entreprise = getEntity(id);

        // null = supprime le contrôle (pas de limite sur cette heure)
        entreprise.setHeureArriveeStandard(dto.getArriveeStandard());
        entreprise.setHeureDepartStandard(dto.getDepartStandard());
        entreprise.setHeureArriveeMinimale(dto.getArriveeMinimale());

        // Validation métier : l'arrivée minimale doit précéder l'arrivée standard si les deux sont définies.
        if (dto.getArriveeMinimale() != null && dto.getArriveeStandard() != null
                && !dto.getArriveeMinimale().isBefore(dto.getArriveeStandard())) {
            throw new IllegalArgumentException(
                    "L'heure d'arrivée minimale doit être avant l'heure d'arrivée standard");
        }

        entreprise = entrepriseRepository.save(entreprise);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Configuration du pointage modifiée pour l'entreprise '" + entreprise.getName() + "'",
                "Entreprise", entreprise.getId(), entreprise.getName());
        log.info("Config pointage entreprise {} mise à jour", id);
        return enrichAndMap(entreprise);
    }

    @Transactional
    public EntrepriseResponseDTO toggleStatut(Long id) {
        Entreprise entreprise = getEntity(id);
        entreprise.setStatut(entreprise.getStatut() == Entreprise.StatutEntreprise.ACTIVE
                ? Entreprise.StatutEntreprise.SUSPENDUE
                : Entreprise.StatutEntreprise.ACTIVE);
        entreprise = entrepriseRepository.save(entreprise);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.TOGGLE_STATUS,
                "Changement de statut de l'entreprise '" + entreprise.getName()
                        + "' → " + entreprise.getStatut(),
                "Entreprise", entreprise.getId(), entreprise.getName());

        return enrichAndMap(entreprise);
    }

    // ─── §2 (5.9) — Comptes ADMIN : création / rôle / reset ───────────────────

    /**
     * Création d'un compte ADMIN supplémentaire (SUPER_ADMIN).
     * Le rôle est FORCÉ à ADMIN — jamais de création d'employé RH par le SUPER_ADMIN (§7).
     */
    @Transactional
    public EmployerResponseDTO createAdmin(Long entrepriseId, AdminCreateRequestDTO dto) {
        Entreprise entreprise = getEntity(entrepriseId);
        String email = dto.getEmail().trim().toLowerCase();
        if (employerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rôle ADMIN introuvable"));

        Employer admin = Employer.builder()
                .name(dto.getName().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(dto.getMotDePasse()))
                .role(adminRole)
                .entreprise(entreprise)
                .actif(true)
                .dateEmbauche(java.time.LocalDate.now())
                .build();
        admin = employerRepository.save(admin);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Création du compte ADMIN '" + admin.getName() + "' pour l'entreprise '"
                        + entreprise.getName() + "'",
                "Employer", admin.getId(), admin.getName());
        log.info("Compte ADMIN créé : {} (entreprise {})", email, entrepriseId);
        return employerMapper.toResponseDTO(admin);
    }

    /**
     * Promouvoir / rétrograder le rôle d'un compte d'entreprise (SUPER_ADMIN).
     * Garde RG-M2-03 : jamais de rétrogradation du dernier admin actif.
     * SUPER_ADMIN interdit comme rôle cible.
     */
    @Transactional
    public EmployerResponseDTO changeAdminRole(Long entrepriseId, Long adminId, String roleName) {
        getEntity(entrepriseId);
        Employer target = employerRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + adminId));
        if (target.getEntreprise() == null || !entrepriseId.equals(target.getEntreprise().getId())) {
            throw new IllegalArgumentException("Ce compte n'appartient pas à cette entreprise");
        }
        String newRole = roleName == null ? "" : roleName.trim().toUpperCase();
        if (newRole.isEmpty()) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        if ("SUPER_ADMIN".equals(newRole)) {
            throw new IllegalArgumentException("Le rôle SUPER_ADMIN ne s'attribue jamais ici");
        }
        Role role = roleRepository.findByName(newRole)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + newRole));

        boolean wasAdmin = "ADMIN".equalsIgnoreCase(target.getRole() != null ? target.getRole().getName() : "");
        boolean isAdminNow = "ADMIN".equals(newRole);
        if (wasAdmin && !isAdminNow && target.isActif()
                && employerRepository.countActiveAdminsByEntrepriseId(entrepriseId) <= 1) {
            throw new IllegalArgumentException(
                    "Impossible de rétrograder le dernier admin actif de cette entreprise. Créez d'abord un autre admin.");
        }

        target.setRole(role);
        target = employerRepository.save(target);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Changement de rôle de '" + target.getName() + "' → " + newRole,
                "Employer", target.getId(), target.getName());
        return employerMapper.toResponseDTO(target);
    }

    /**
     * Réinitialisation du mot de passe d'un compte ADMIN (SUPER_ADMIN).
     * Mot de passe temporaire renvoyé hors audit ; drapeau motDePasseTemporaire levé
     * (reposé au prochain changement de mot de passe par l'utilisateur).
     */
    @Transactional
    public ResetPasswordResponseDTO resetAdminPassword(Long entrepriseId, Long adminId) {
        getEntity(entrepriseId);
        Employer admin = employerRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + adminId));
        if (admin.getEntreprise() == null || !entrepriseId.equals(admin.getEntreprise().getId())) {
            throw new IllegalArgumentException("Ce compte n'appartient pas à cette entreprise");
        }
        if (!"ADMIN".equalsIgnoreCase(admin.getRole() != null ? admin.getRole().getName() : "")) {
            throw new IllegalArgumentException("Ce compte n'est pas un administrateur");
        }

        String temp = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        admin.setMotDePasse(passwordEncoder.encode(temp));
        admin.setMotDePasseTemporaire(true);
        employerRepository.save(admin);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.RESET_PASSWORD,
                "Réinitialisation du mot de passe de l'admin '" + admin.getName()
                        + "' (mot de passe temporaire remis hors audit)",
                "Employer", admin.getId(), admin.getName());
        log.info("Mot de passe réinitialisé pour l'admin {} (entreprise {})", admin.getEmail(), entrepriseId);
        return new ResetPasswordResponseDTO(admin.getId(), admin.getEmail(), temp);
    }

    // ─── Utilitaires ─────────────────────────────────────────

    /** Vérifie que l'utilisateur courant a le droit de modifier l'entreprise donnée
     *  (SUPER_ADMIN : tout ; ADMIN : sa propre entreprise uniquement). */
    private void checkAdminScope(Long entrepriseId) {
        if (entrepriseContextService.isSuperAdmin()) return;
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        if (scope == null || !scope.equals(entrepriseId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez configurer que votre propre entreprise");
        }
    }

    /** Convertit les lignes [entrepriseId, count] en Map. */
    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new java.util.HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    /** Convertit les lignes [entrepriseId, sum(montant)] en Map. */
    private Map<Long, BigDecimal> toSumMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new java.util.HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) map.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }
        return map;
    }

    private EntrepriseResponseDTO enrichAndMap(Entreprise entreprise) {
        EntrepriseResponseDTO dto = entrepriseMapper.toResponseDTO(entreprise);
        dto.setNombreEmployes(employerRepository.countByEntrepriseId(entreprise.getId()));
        return dto;
    }
}