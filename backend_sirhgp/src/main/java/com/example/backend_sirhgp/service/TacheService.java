package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.TacheRequestDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.event.TaskAssignedEvent;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.TacheMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.model.Sucursal;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.repository.CommentaireTacheRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import com.example.backend_sirhgp.repository.DemandePermissionRepository;
import com.example.backend_sirhgp.repository.SucursalRepository;
import com.example.backend_sirhgp.service.EntrepriseContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VERSION FINALE ET CORRECTE
 * - Toutes les corrections de la première version (DTOs, créateur JWT, pagination, findByIdWithRelations, dureeExecution en MINUTES, commentairesRecents limités, etc.)
 * - Intégration complète de la logique d'événements de la deuxième version (TaskAssignedEvent sur création et modification d'assignation)
 * - Gestion propre de l'ancien/nouvel assignee dans update()
 * - Le reste des méthodes (findAll, findById, findByAssigneeId, findByProjetId, updateStatut, delete) est inchangé et cohérent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TacheService {

    private final TacheRepository tacheRepository;
    private final EmployerRepository employerRepository;
    private final ProjetRepository projetRepository;
    private final CommentaireTacheRepository commentaireRepository;
    private final TacheMapper tacheMapper;
    private final CommentaireService commentaireService;
    private final ApplicationEventPublisher eventPublisher; // AJOUT pour la publication d'événements
    private final DemandePermissionRepository demandePermissionRepository;
    private final SucursalRepository sucursalRepository;
    private final EntrepriseContextService entrepriseContextService;
    private final TacheAccessService tacheAccessService;

    @Transactional(readOnly = true)
    public Map<String, Object> getMonEtat() {
        Employer current = getCurrentUser();
        boolean roleGestion = current.getRole() != null && List.of("SUPER_ADMIN", "ADMIN", "MANAGER", "LEAD")
                .contains(current.getRole().getName());
        boolean enConge = !demandePermissionRepository.findAbsencesApprouvees(
                current.getId(), LocalDate.now(), LocalDate.now()).isEmpty();
        return Map.of(
                "peutReagir", !roleGestion && !enConge,
                "roleGestion", roleGestion,
                "enConge", enConge,
                "raison", enConge ? "Une absence approuvée est en cours aujourd'hui." : ""
        );
    }

    @Transactional
    public TacheResponseDTO create(TacheRequestDTO dto) {
        // CORRECTION : créateur = utilisateur connecté
        Employer createur = getCurrentUser();

        Tache tache = tacheMapper.toEntity(dto);
        tache.setCreateur(createur);

        Employer assignee = null;
        if (dto.getAssigneeId() != null) {
            assignee = employerRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee non trouvé : " + dto.getAssigneeId()));
            tache.setAssignee(assignee);
        }

        if (dto.getProjetId() == null) throw new IllegalArgumentException("Le projet est obligatoire pour créer une tâche.");
        Projet projet = projetRepository.findById(dto.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
        configurerPerimetreTache(tache, createur, projet, dto.getSucursalId(), assignee);
        tache.setProjet(projet);

        if (tache.getStatut() == Tache.StatutTache.EN_COURS && tache.getDateDebut() == null) {
            tache.setDateDebut(LocalDateTime.now());
        }

        Tache saved = tacheRepository.save(tache);
        log.info("Tâche créée : '{}' (ID {}) par {}", saved.getTitre(), saved.getId(), createur.getEmail());

        // PUBLICATION ÉVÉNEMENT SI ASSIGNEE
        if (assignee != null) {
            eventPublisher.publishEvent(new TaskAssignedEvent(this, saved, assignee, createur));
        }

        return enrich(tacheMapper.toResponseDTO(saved), saved);
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<TacheResponseDTO> findAll(Pageable pageable) {
        Employer current = getCurrentUser();
        List<TacheResponseDTO> visibles = tacheRepository.findAllWithRelations().stream()
                .filter(t -> tacheVisiblePourUtilisateur(t, current))
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t)).toList();
        return page(visibles, pageable);
    }

    @Transactional(readOnly = true)
    public TacheResponseDTO findById(Long id) {
        // CORRECTION : findByIdWithRelations pour éviter N+1
        Tache tache = tacheRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));
        verifierTacheAccessible(tache, getCurrentUser());
        return enrich(tacheMapper.toResponseDTO(tache), tache);
    }

    @Transactional(readOnly = true)
    public List<TacheResponseDTO> findByAssigneeId(Long assigneeId) {
        Employer current = getCurrentUser();
        if (isSecretary(current) && !current.getId().equals(assigneeId)) throw new org.springframework.security.access.AccessDeniedException("Vous ne pouvez consulter que les tâches qui vous sont assignées.");
        Employer assignee = employerRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + assigneeId));
        verifierEmployeAccessible(current, assignee);
        return tacheRepository.findByAssigneeIdWithRelations(assigneeId).stream()
                .filter(t -> tacheVisiblePourUtilisateur(t, current))
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponseDTO> findByProjetId(Long projetId) {
        Employer current = getCurrentUser();
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        verifierProjetAccessible(current, projet);
        // CORRECTION : query avec JOIN FETCH sur assignee
        return tacheRepository.findByProjetIdWithAssignee(projetId).stream()
                .filter(t -> tacheVisiblePourUtilisateur(t, current))
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t))
                .collect(Collectors.toList());
    }

    @Transactional
    public TacheResponseDTO update(Long id, TacheRequestDTO dto) {
        Tache tache = tacheRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));
        Employer current = getCurrentUser();
        verifierTacheAccessible(tache, current);
        verifierModificationSecretaire(tache, current);

        Employer oldAssignee = tache.getAssignee(); // Sauvegarde ancien assignee

        tacheMapper.updateEntity(tache, dto);

        Employer newAssignee = null;
        if (dto.getAssigneeId() != null) {
            newAssignee = employerRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee non trouvé : " + dto.getAssigneeId()));
            tache.setAssignee(newAssignee);
        } else {
            tache.setAssignee(null);
        }

        if (dto.getProjetId() == null) throw new IllegalArgumentException("Le projet est obligatoire pour une tâche.");
        Projet projet = projetRepository.findById(dto.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
        configurerPerimetreTache(tache, current, projet, dto.getSucursalId(), newAssignee);
        tache.setProjet(projet);

        updateDatesStatut(tache);
        Tache updated = tacheRepository.save(tache);

        // PUBLICATION ÉVÉNEMENT SI ASSIGNEE A CHANGÉ (ajouté ou modifié)
        boolean assigneeChanged = (oldAssignee == null && newAssignee != null) ||
                (oldAssignee != null && newAssignee != null && !oldAssignee.getId().equals(newAssignee.getId()));
        if (assigneeChanged && newAssignee != null) {
            Employer currentUser = getCurrentUser();
            eventPublisher.publishEvent(new TaskAssignedEvent(this, updated, newAssignee, currentUser));
        }

        return enrich(tacheMapper.toResponseDTO(updated), updated);
    }

    @Transactional
    public TacheResponseDTO updateStatut(Long id, Tache.StatutTache statut) {
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));
        verifierTacheAccessible(tache, getCurrentUser());
        verifierModificationSecretaire(tache, getCurrentUser());
        tache.setStatut(statut);
        updateDatesStatut(tache);
        return enrich(tacheMapper.toResponseDTO(tacheRepository.save(tache)), tache);
    }

    @Transactional
    public void delete(Long id) {
        Employer current = getCurrentUser();
        if (isSecretary(current)) throw new org.springframework.security.access.AccessDeniedException("La suppression des tâches est interdite au rôle SECRETAIRE.");
        if (!tacheRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tâche non trouvée : " + id);
        }
        Tache tache = tacheRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));
        verifierTacheAccessible(tache, current);
        tacheRepository.deleteById(id);
        log.info("Tâche supprimée : {}", id);
    }

    private boolean isSecretary(Employer user) { return user.getRole() != null && "SECRETAIRE".equals(user.getRole().getName()); }
    private boolean isAdmin(Employer user) { return user.getRole() != null && "ADMIN".equals(user.getRole().getName()); }
    private boolean isSuperAdmin(Employer user) { return user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName()); }

    private void configurerPerimetreTache(Tache tache, Employer createur, Projet projet,
                                          Long sucursalId, Employer assignee) {
        verifierProjetAccessible(createur, projet);
        Entreprise entreprise = projet.getEntreprise();
        if (entreprise == null) throw new IllegalArgumentException("Le projet doit être rattaché à une entreprise.");

        Sucursal sucursal = null;
        Long cibleSucursalId = sucursalId != null ? sucursalId
                : projet.getSucursal() != null ? projet.getSucursal().getId() : null;
        if (cibleSucursalId != null) {
            sucursal = sucursalRepository.findById(cibleSucursalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Succursale introuvable : " + cibleSucursalId));
            if (sucursal.getEntreprise() == null || !entreprise.getId().equals(sucursal.getEntreprise().getId()))
                throw new IllegalArgumentException("La succursale choisie ne correspond pas à l'entreprise du projet.");
        }
        if (projet.getSucursal() != null && (sucursal == null || !projet.getSucursal().getId().equals(sucursal.getId())))
            throw new IllegalArgumentException("La succursale doit correspondre à celle du projet.");

        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (sucursal == null || !scopeSucursal.equals(sucursal.getId())))
            throw new org.springframework.security.access.AccessDeniedException("Action hors de votre succursale.");
        if (assignee != null) {
            if (assignee.getEntreprise() == null || !entreprise.getId().equals(assignee.getEntreprise().getId()))
                throw new IllegalArgumentException("La personne assignée doit appartenir à l'entreprise du projet.");
            if (sucursal != null && (assignee.getSucursal() == null || !sucursal.getId().equals(assignee.getSucursal().getId())))
                throw new IllegalArgumentException("La personne assignée doit appartenir à la succursale de la tâche.");
        }
        tache.setEntreprise(entreprise);
        tache.setSucursal(sucursal);
    }

    private void verifierProjetAccessible(Employer user, Projet projet) {
        if (isSuperAdmin(user)) return;
        if (user.getEntreprise() == null || projet.getEntreprise() == null
                || !user.getEntreprise().getId().equals(projet.getEntreprise().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Projet hors de votre entreprise.");
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (projet.getSucursal() == null
                || !scopeSucursal.equals(projet.getSucursal().getId())))
            throw new org.springframework.security.access.AccessDeniedException("Projet hors de votre succursale.");
        String role = user.getRole() != null ? user.getRole().getName() : "";
        if ("LEAD".equals(role)) {
            List<Long> depts = entrepriseContextService.getScopeDepartementIds();
            if (!depts.isEmpty() && (projet.getDepartements() == null || projet.getDepartements().stream()
                    .noneMatch(d -> depts.contains(d.getId()))))
                throw new org.springframework.security.access.AccessDeniedException("Projet hors de vos départements.");
        }
        if (("STAFF".equals(role) || "STAGIAIRE".equals(role))
                && (projet.getChefProjet() == null || !user.getId().equals(projet.getChefProjet().getId()))
                && (projet.getMembres() == null || projet.getMembres().stream().noneMatch(e -> user.getId().equals(e.getId()))))
            throw new org.springframework.security.access.AccessDeniedException("Projet non attribué à votre profil.");
    }

    private void verifierEmployeAccessible(Employer user, Employer employee) {
        if (isSuperAdmin(user)) return;
        if (user.getEntreprise() == null || employee.getEntreprise() == null
                || !user.getEntreprise().getId().equals(employee.getEntreprise().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Employé hors de votre entreprise.");
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (employee.getSucursal() == null
                || !scopeSucursal.equals(employee.getSucursal().getId())))
            throw new org.springframework.security.access.AccessDeniedException("Employé hors de votre succursale.");
    }

    private boolean estTacheDansScope(Employer user, Tache tache) {
        if (isSuperAdmin(user)) return true;
        if (user.getEntreprise() == null || tache.getEntreprise() == null
                || !user.getEntreprise().getId().equals(tache.getEntreprise().getId())) return false;
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        return scopeSucursal == null || (tache.getSucursal() != null
                && scopeSucursal.equals(tache.getSucursal().getId()));
    }

    private boolean tacheVisiblePourUtilisateur(Tache tache, Employer user) {
        return tacheAccessService.peutVoir(tache, user);
    }

    private void verifierTacheAccessible(Tache tache, Employer user) {
        if (!tacheVisiblePourUtilisateur(tache, user))
            throw new ResourceNotFoundException("Tâche non trouvée : " + tache.getId());
    }
    private boolean dansSuccursale(Employer user, Tache t) {
        return user.getEntreprise() != null && user.getSucursal() != null && t.getEntreprise() != null && t.getSucursal() != null
                && user.getEntreprise().getId().equals(t.getEntreprise().getId())
                && user.getSucursal().getId().equals(t.getSucursal().getId());
    }
    private void verifierLectureSecretaire(Tache t, Employer user) {
        if (isSecretary(user) && !dansSuccursale(user, t)) throw new ResourceNotFoundException("Tâche non trouvée : " + t.getId());
    }
    private void verifierModificationSecretaire(Tache t, Employer user) {
        if (isSecretary(user) && (t.getCreateur() == null || !user.getId().equals(t.getCreateur().getId()) || !dansSuccursale(user, t)))
            throw new org.springframework.security.access.AccessDeniedException("Vous pouvez modifier uniquement vos tâches de votre succursale.");
    }
    private void verifierMemeSuccursale(Employer user, com.example.backend_sirhgp.model.Entreprise ent,
                                        com.example.backend_sirhgp.model.Sucursal suc) {
        if (user.getEntreprise() == null || user.getSucursal() == null || ent == null || suc == null
                || !user.getEntreprise().getId().equals(ent.getId()) || !user.getSucursal().getId().equals(suc.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Les personnes, projets et tâches doivent rester dans votre succursale.");
    }
    private Page<TacheResponseDTO> page(List<TacheResponseDTO> items, Pageable pageable) {
        if (pageable.isUnpaged()) return new PageImpl<>(items);
        int from = Math.min((int) pageable.getOffset(), items.size());
        int to = Math.min(from + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(from, to), pageable, items.size());
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private TacheResponseDTO enrich(TacheResponseDTO dto, Tache tache) {
        // Indicateurs temporels
        if (tache.getDateEcheance() != null) {
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), tache.getDateEcheance());
            dto.setJoursRestants(joursRestants);
            dto.setEstEnRetard(joursRestants < 0 && tache.getStatut() != Tache.StatutTache.TERMINEE);
        }

        // CORRECTION : dureeExecution en MINUTES (pas en jours)
        if (tache.getDateDebut() != null && tache.getDateFin() != null) {
            dto.setDureeExecution(ChronoUnit.MINUTES.between(tache.getDateDebut(), tache.getDateFin()));
        }

        // Compteur de commentaires
        dto.setNombreCommentaires(commentaireRepository.countByTacheId(tache.getId()));

        // Derniers commentaires (max 3) — évite de charger toute la collection
        dto.setCommentairesRecents(
                commentaireService.findRecentsByTacheId(tache.getId(), 3)
        );

        // Pourcentage de completion selon le statut
        dto.setPourcentageCompletion(switch (tache.getStatut()) {
            case TERMINEE -> 100.0;
            case EN_REVUE  -> 75.0;
            case EN_COURS  -> 50.0;
            case BLOQUEE   -> 10.0;
            default        -> 0.0;
        });

        return dto;
    }

    private void updateDatesStatut(Tache tache) {
        if (tache.getStatut() == Tache.StatutTache.EN_COURS && tache.getDateDebut() == null) {
            tache.setDateDebut(LocalDateTime.now());
        }
        if (tache.getStatut() == Tache.StatutTache.TERMINEE && tache.getDateFin() == null) {
            tache.setDateFin(LocalDateTime.now());
        }
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }
}
