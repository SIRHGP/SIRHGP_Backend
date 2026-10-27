package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.dashboard.ProjetDashboardDTO;
import com.example.backend_sirhgp.dto.request.ProjetRequestDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.ProjetMapper;
import com.example.backend_sirhgp.model.*;
import com.example.backend_sirhgp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final EmployerRepository employerRepository;
    private final DepartementRepository departementRepository;
    private final TacheRepository tacheRepository;
    private final FactureRepository factureRepository;
    private final ProjetMapper projetMapper;
    private final EntrepriseContextService entrepriseContextService;
    private final SucursalRepository sucursalRepository;
    private final TacheAccessService tacheAccessService;

    @Transactional
    public ProjetResponseDTO create(ProjetRequestDTO dto) {
        verifierRoleGestion();
        if (projetRepository.existsByNom(dto.getNom())) {
            throw new IllegalArgumentException("Un projet avec ce nom existe déjà : " + dto.getNom());
        }
        Projet projet = projetMapper.toEntity(dto);
        if (projet.getCodeProjet() == null || projet.getCodeProjet().isBlank()) {
            projet.setCodeProjet("PROJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (projetRepository.existsByCodeProjet(projet.getCodeProjet())) {
            throw new IllegalArgumentException("Code projet déjà utilisé : " + projet.getCodeProjet());
        }
        resolveRelations(projet, dto);
        appliquerScopeCreation(projet, dto);
        verifierRelationsProjet(projet);
        projet.updateBudget();
        Projet saved = projetRepository.save(projet);
        log.info("Projet créé : {} ({})", saved.getNom(), saved.getCodeProjet());
        return enrichAndMap(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProjetResponseDTO> findAll(Pageable pageable) {
        List<ProjetResponseDTO> dtos = projetRepository.findAll().stream()
                .filter(this::projetVisiblePourUtilisateur)
                .map(this::enrichAndMap).toList();
        if (pageable.isUnpaged()) return new org.springframework.data.domain.PageImpl<>(dtos);
        int from = Math.min((int) pageable.getOffset(), dtos.size());
        int to = Math.min(from + pageable.getPageSize(), dtos.size());
        return new org.springframework.data.domain.PageImpl<>(dtos.subList(from, to), pageable, dtos.size());
    }

    @Transactional(readOnly = true)
    public ProjetResponseDTO findById(Long id) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        verifierLectureScope(projet);
        return enrichAndMap(projet);
    }

    @Transactional
    public ProjetResponseDTO update(Long id, ProjetRequestDTO dto) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        verifierGestionProjet(projet);
        projetMapper.updateEntity(projet, dto);
        resolveRelations(projet, dto);
        appliquerScopeModification(projet, dto);
        verifierRelationsProjet(projet);
        if (projet.getStatut() == Projet.StatutProjet.TERMINE && projet.getDateFinReelle() == null) {
            projet.setDateFinReelle(LocalDate.now());
        }
        projet.updateBudget();
        projet.verifierEtMettreAJourRetard();
        return enrichAndMap(projetRepository.save(projet));
    }

    private void verifierLectureScope(Projet projet) {
        if (!projetVisiblePourUtilisateur(projet))
            throw new ResourceNotFoundException("Projet non trouvé");
    }

    private boolean projetVisiblePourUtilisateur(Projet projet) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) return false;
        String role = current.getRole() != null ? current.getRole().getName() : "";
        if ("SUPER_ADMIN".equals(role)) return false;
        if (current.getEntreprise() == null || projet.getEntreprise() == null
                || !current.getEntreprise().getId().equals(projet.getEntreprise().getId())) return false;

        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (projet.getSucursal() == null
                || !scopeSucursal.equals(projet.getSucursal().getId()))) return false;
        if ("SECRETAIRE".equals(role) && (current.getSucursal() == null || projet.getSucursal() == null
                || !current.getSucursal().getId().equals(projet.getSucursal().getId()))) return false;

        if ("LEAD".equals(role)) {
            List<Long> departements = entrepriseContextService.getScopeDepartementIds();
            if (!departements.isEmpty() && (projet.getDepartements() == null || projet.getDepartements().stream()
                    .noneMatch(d -> departements.contains(d.getId())))) return false;
        }
        if ("STAFF".equals(role) || "STAGIAIRE".equals(role)) {
            boolean membre = projet.getMembres() != null && projet.getMembres().stream()
                    .anyMatch(e -> current.getId().equals(e.getId()));
            boolean chef = projet.getChefProjet() != null && current.getId().equals(projet.getChefProjet().getId());
            if (!membre && !chef) return false;
        }
        return true;
    }

    private void verifierGestionProjet(Projet projet) {
        verifierRoleGestion();
        verifierLectureScope(projet);
    }

    private void verifierRoleGestion() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = current != null && current.getRole() != null ? current.getRole().getName() : "";
        if (!List.of("ADMIN", "MANAGER", "LEAD").contains(role))
            throw new org.springframework.security.access.AccessDeniedException("La gestion des projets est réservée aux rôles ADMIN, MANAGER et LEAD.");
    }

    private void verifierRelationsProjet(Projet projet) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null || current.getEntreprise() == null || projet.getEntreprise() == null
                || !current.getEntreprise().getId().equals(projet.getEntreprise().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Le projet et ses rattachements doivent appartenir à votre entreprise.");
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (projet.getSucursal() == null || !scopeSucursal.equals(projet.getSucursal().getId())))
            throw new org.springframework.security.access.AccessDeniedException("Le projet doit rester dans votre succursale.");

        if (projet.getChefProjet() != null) verifierEmployeReference(projet.getChefProjet(), current, "chef de projet");
        if (projet.getMembres() != null) projet.getMembres().forEach(m -> verifierEmployeReference(m, current, "membre"));
        if (projet.getDepartements() != null) {
            List<Long> departementsLead = entrepriseContextService.getScopeDepartementIds();
            for (Departement d : projet.getDepartements()) {
                if (d.getEntreprise() == null || !current.getEntreprise().getId().equals(d.getEntreprise().getId()))
                    throw new org.springframework.security.access.AccessDeniedException("Un département du projet est hors de votre entreprise.");
                if (entrepriseContextService.isLead() && (!departementsLead.contains(d.getId())))
                    throw new org.springframework.security.access.AccessDeniedException("Un département du projet est hors de votre périmètre.");
            }
        }
        if (entrepriseContextService.isLead()) {
            List<Long> ids = entrepriseContextService.getScopeDepartementIds();
            if (ids.isEmpty() || projet.getDepartements() == null || projet.getDepartements().stream().noneMatch(d -> ids.contains(d.getId())))
                throw new org.springframework.security.access.AccessDeniedException("Un LEAD doit rattacher le projet à l'un de ses départements supervisés.");
        }
    }

    private void verifierEmployeReference(Employer cible, Employer current, String usage) {
        if (current == null || current.getEntreprise() == null || cible.getEntreprise() == null
                || !current.getEntreprise().getId().equals(cible.getEntreprise().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Le " + usage + " doit appartenir à votre entreprise.");
        Long sucursalScope = entrepriseContextService.getScopeSucursalId();
        if (sucursalScope != null && (cible.getSucursal() == null || !sucursalScope.equals(cible.getSucursal().getId())))
            throw new org.springframework.security.access.AccessDeniedException("Le " + usage + " doit appartenir à votre succursale.");
        if (entrepriseContextService.isLead() && !entrepriseContextService.employeDansLePerimetre(cible.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Le " + usage + " doit appartenir à l'un de vos départements supervisés.");
    }

    private void verifierDepartementReference(Departement departement) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null || current.getEntreprise() == null || departement.getEntreprise() == null
                || !current.getEntreprise().getId().equals(departement.getEntreprise().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Le département doit appartenir à votre entreprise.");
        if (entrepriseContextService.isLead() && !entrepriseContextService.getScopeDepartementIds().contains(departement.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Le département est hors de votre périmètre.");
    }

    private void appliquerScopeCreation(Projet projet, ProjetRequestDTO dto) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        Entreprise entreprise = entrepriseContextService.getEntrepriseOfCurrentUser();
        if (entreprise != null) projet.setEntreprise(entreprise);
        Long sucId = dto.getSucursalId() != null ? dto.getSucursalId() : entrepriseContextService.getScopeSucursalId();
        if (sucId != null) {
            Sucursal suc = sucursalRepository.findById(sucId)
                    .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée : " + sucId));
            if (entreprise != null && (suc.getEntreprise() == null || !entreprise.getId().equals(suc.getEntreprise().getId())))
                throw new org.springframework.security.access.AccessDeniedException("Succursale hors de votre entreprise.");
            if (current != null && current.getSucursal() != null && !current.getSucursal().getId().equals(suc.getId()))
                throw new org.springframework.security.access.AccessDeniedException("Vous ne pouvez utiliser que votre succursale.");
            projet.setSucursal(suc);
        }
    }

    private void appliquerScopeModification(Projet projet, ProjetRequestDTO dto) {
        if (dto.getSucursalId() == null) return;
        Long scope = entrepriseContextService.getScopeSucursalId();
        if (scope != null && !scope.equals(dto.getSucursalId()))
            throw new org.springframework.security.access.AccessDeniedException("Vous ne pouvez déplacer ce projet hors de votre succursale.");
        Sucursal suc = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée : " + dto.getSucursalId()));
        projet.setSucursal(suc);
        projet.setEntreprise(suc.getEntreprise());
    }

    @Transactional
    public void delete(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        verifierGestionProjet(projet);
        projetRepository.delete(projet);
        log.info("Projet supprimé : {}", id);
    }

    @Transactional
    public ProjetResponseDTO updateStatut(Long id, Projet.StatutProjet statut) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        verifierGestionProjet(projet);
        projet.setStatut(statut);
        if (statut == Projet.StatutProjet.TERMINE && projet.getDateFinReelle() == null) {
            projet.setDateFinReelle(LocalDate.now());
        }
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO addMembre(Long projetId, Long membreId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        verifierGestionProjet(projet);
        Employer membre = employerRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé : " + membreId));
        verifierEmployeReference(membre, entrepriseContextService.getCurrentEmployer(), "membre");
        projet.addMembre(membre);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO removeMembre(Long projetId, Long membreId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        verifierGestionProjet(projet);
        Employer membre = employerRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé : " + membreId));
        verifierEmployeReference(membre, entrepriseContextService.getCurrentEmployer(), "membre");
        projet.removeMembre(membre);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO addDepartement(Long projetId, Long deptId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        verifierGestionProjet(projet);
        Departement dept = departementRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + deptId));
        verifierDepartementReference(dept);
        projet.addDepartement(dept);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO removeDepartement(Long projetId, Long deptId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        verifierGestionProjet(projet);
        Departement dept = departementRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + deptId));
        verifierDepartementReference(dept);
        projet.removeDepartement(dept);
        return enrichAndMap(projetRepository.save(projet));
    }

    // CORRECTION : méthode manquante appelée depuis ProjetController
    @Transactional(readOnly = true)
    public List<ProjetResponseDTO> findByDepartement(Long departementId) {
        return projetRepository.findByDepartementId(departementId).stream()
                .filter(this::projetVisiblePourUtilisateur)
                .map(this::enrichAndMap)
                .collect(Collectors.toList());
    }

    // CORRECTION : méthode manquante appelée depuis ProjetController
    @Transactional(readOnly = true)
    public List<ProjetResponseDTO> findProjetsEnRetard() {
        return projetRepository.findByDateFinPrevueBeforeAndStatutNot(
                        LocalDate.now(), Projet.StatutProjet.TERMINE)
                .stream().filter(this::projetVisiblePourUtilisateur).map(this::enrichAndMap).collect(Collectors.toList());
    }

    // ─── Dashboard ────────────────────────────────────────────

    // CORRECTION : méthodes manquantes appelées depuis ProjetController
    @Transactional(readOnly = true)
    public ProjetDashboardDTO getGlobalDashboardStats() {
        List<Projet> visibles = projetRepository.findAll().stream().filter(this::projetVisiblePourUtilisateur).toList();
        java.math.BigDecimal budgetAlloue = visibles.stream().map(Projet::getBudgetAlloue)
                .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal budgetConsomme = visibles.stream().map(Projet::getBudgetConsomme)
                .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        List<Tache> tachesVisibles = visibles.stream().flatMap(p -> tachesVisiblesProjet(p.getId()).stream()).toList();
        String role = entrepriseContextService.currentRole();
        boolean canViewFinancial = "ADMIN".equals(role) || "MANAGER".equals(role);
        long totalTaches = tachesVisibles.size();
        long terminees = tachesVisibles.stream().filter(t -> t.getStatut() == Tache.StatutTache.TERMINEE).count();
        long enCours = tachesVisibles.stream().filter(t -> t.getStatut() == Tache.StatutTache.EN_COURS).count();
        return ProjetDashboardDTO.builder()
                .totalProjets(visibles.size())
                .projetsEnCours((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.EN_COURS).count())
                .projetsTermines((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.TERMINE).count())
                .projetsEnRetard((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.EN_RETARD).count())
                .projetsPlanifies((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.PLANIFIE).count())
                .projetsSuspendus((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.SUSPENDU).count())
                .projetsAnnules((int) visibles.stream().filter(p -> p.getStatut() == Projet.StatutProjet.ANNULE).count())
                .budgetTotalAlloue(canViewFinancial ? budgetAlloue : null)
                .budgetTotalConsomme(canViewFinancial ? budgetConsomme : null)
                .totalTaches((int) totalTaches)
                .tachesTerminees((int) terminees)
                .tachesEnCours((int) enCours)
                .build();
    }

    @Transactional(readOnly = true)
    public ProjetDashboardDTO.ProjetDetailView getProjetDashboardStats(Long id) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        verifierLectureScope(projet);
        List<Tache> taches = tachesVisiblesProjet(id);
        ProjetResponseDTO scopedProject = enrichAndMap(projet);
        return ProjetDashboardDTO.ProjetDetailView.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .codeProjet(projet.getCodeProjet())
                .statut(projet.getStatut())
                .dateDebut(projet.getDateDebut())
                .dateFinPrevue(projet.getDateFinPrevue())
                .progression(projet.getProgression())
                .totalTaches(taches.size())
                .tachesTerminees((int) taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.TERMINEE).count())
                .tachesEnCours((int) taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.EN_COURS).count())
                .nombreMembres(scopedProject.getNombreMembres().intValue())
                .chefProjetId(scopedProject.getChefProjet() != null ? scopedProject.getChefProjet().getId() : null)
                .chefProjetNom(scopedProject.getChefProjet() != null ? scopedProject.getChefProjet().getName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProjetDashboardDTO.StatutStatDTO> getStatistiquesParStatut() {
        List<Projet> visibles = projetRepository.findAll().stream().filter(this::projetVisiblePourUtilisateur).toList();
        long total = visibles.size();
        return Arrays.stream(Projet.StatutProjet.values()).map(statut -> {
            long count = visibles.stream().filter(p -> p.getStatut() == statut).count();
            return ProjetDashboardDTO.StatutStatDTO.builder()
                    .statut(statut)
                    .libelle(statut.name())
                    .count((int) count)
                    .pourcentage(total > 0 ? (count * 100.0 / total) : 0.0)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjetDashboardDTO.BudgetGlobalDTO getStatistiquesBudget() {
        List<Projet> visibles = projetRepository.findAll().stream().filter(this::projetVisiblePourUtilisateur).toList();
        String role = entrepriseContextService.currentRole();
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            return ProjetDashboardDTO.BudgetGlobalDTO.builder()
                    .totalAlloue(java.math.BigDecimal.ZERO)
                    .totalConsomme(java.math.BigDecimal.ZERO)
                    .totalRestant(java.math.BigDecimal.ZERO)
                    .tauxConsommation(0.0)
                    .build();
        }
        var alloue = visibles.stream().map(Projet::getBudgetAlloue).filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var consomme = visibles.stream().map(Projet::getBudgetConsomme).filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var restant = alloue != null && consomme != null
                ? alloue.subtract(consomme) : java.math.BigDecimal.ZERO;
        double taux = (alloue != null && alloue.compareTo(java.math.BigDecimal.ZERO) > 0)
                ? consomme.divide(alloue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        return ProjetDashboardDTO.BudgetGlobalDTO.builder()
                .totalAlloue(alloue)
                .totalConsomme(consomme)
                .totalRestant(restant)
                .tauxConsommation(taux)
                .build();
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private void resolveRelations(Projet projet, ProjetRequestDTO dto) {
        if (dto.getChefProjetId() != null) {
            projet.setChefProjet(employerRepository.findById(dto.getChefProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chef non trouvé : " + dto.getChefProjetId())));
        }
        if (dto.getMembresIds() != null) {
            Set<Employer> membres = dto.getMembresIds().stream()
                    .map(mid -> employerRepository.findById(mid)
                            .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé : " + mid)))
                    .collect(Collectors.toSet());
            projet.setMembres(membres);
        }
        if (dto.getDepartementIds() != null) {
            Set<Departement> depts = dto.getDepartementIds().stream()
                    .map(did -> departementRepository.findById(did)
                            .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + did)))
                    .collect(Collectors.toSet());
            projet.setDepartements(depts);
        }
    }

    private ProjetResponseDTO enrichAndMap(Projet projet) {
        ProjetResponseDTO dto = projetMapper.toResponseDTO(projet);
        Employer current = entrepriseContextService.getCurrentEmployer();
        String role = current != null && current.getRole() != null ? current.getRole().getName() : "";
        boolean canViewFinancial = "ADMIN".equals(role) || "MANAGER".equals(role);
        Long id = projet.getId();
        List<Tache> tasks = tachesVisiblesProjet(id);
        dto.setNombreTaches((long) tasks.size());
        dto.setNombreTachesTerminees(tasks.stream().filter(t -> t.getStatut() == Tache.StatutTache.TERMINEE).count());
        dto.setNombreTachesEnCours(tasks.stream().filter(t -> t.getStatut() == Tache.StatutTache.EN_COURS).count());
        dto.setNombreTachesEnRetard(tasks.stream().filter(t -> t.getDateEcheance() != null
                && t.getDateEcheance().isBefore(LocalDate.now()) && t.getStatut() != Tache.StatutTache.TERMINEE).count());

        Long branchScope = entrepriseContextService.getScopeSucursalId();
        if (branchScope != null && dto.getMembres() != null) {
            dto.setMembres(dto.getMembres().stream().filter(m -> branchScope.equals(m.getSucursalId()))
                    .collect(Collectors.toSet()));
            if (dto.getChefProjet() != null && !branchScope.equals(dto.getChefProjet().getSucursalId())) dto.setChefProjet(null);
        }
        if ("LEAD".equals(role)) {
            List<Long> departments = entrepriseContextService.getScopeDepartementIds();
            if (!departments.isEmpty()) {
                if (dto.getDepartements() != null) dto.setDepartements(dto.getDepartements().stream()
                        .filter(d -> departments.contains(d.getId())).collect(Collectors.toSet()));
                if (dto.getMembres() != null) dto.setMembres(dto.getMembres().stream()
                        .filter(m -> departments.contains(m.getDepartementId())).collect(Collectors.toSet()));
                if (dto.getChefProjet() != null && !departments.contains(dto.getChefProjet().getDepartementId())) dto.setChefProjet(null);
            }
        }
        dto.setNombreMembres((long) (dto.getMembres() == null ? 0 : dto.getMembres().size()));
        dto.setNombreDepartements((long) (dto.getDepartements() == null ? 0 : dto.getDepartements().size()));
        if (!canViewFinancial) {
            dto.setBudgetAlloue(null);
            dto.setBudgetConsomme(null);
            dto.setBudgetRestant(null);
            dto.setNombreFactures(null);
            dto.setNombreFacturesPayees(null);
            dto.setNombreFacturesEnAttente(null);
            dto.setMontantTotalFactures(null);
            dto.setMontantPayeFactures(null);
            dto.setMontantRestantFactures(null);
            dto.setDepassementBudget(null);
        } else {
            dto.setNombreFactures(factureRepository.countByProjetId(id));
            dto.setMontantTotalFactures(factureRepository.sumMontantTTCByProjetId(id));
            dto.setMontantPayeFactures(factureRepository.sumMontantPayeByProjetId(id));
        }
        if (projet.getDateFinPrevue() != null) {
            long jours = ChronoUnit.DAYS.between(LocalDate.now(), projet.getDateFinPrevue());
            dto.setJoursRestants(jours);
            dto.setEstEnRetard(jours < 0 && projet.getStatut() != Projet.StatutProjet.TERMINE);
        }
        return dto;
    }

    private List<Tache> tachesVisiblesProjet(Long projetId) {
        Employer current = entrepriseContextService.getCurrentEmployer();
        return tacheRepository.findByProjetIdWithScopeRelations(projetId).stream()
                .filter(t -> tacheAccessService.peutVoir(t, current)).toList();
    }
}
