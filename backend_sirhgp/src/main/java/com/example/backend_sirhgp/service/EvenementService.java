package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.EvenementRequestDTO;
import com.example.backend_sirhgp.dto.response.EvenementResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.EvenementMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Evenement;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.EvenementRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import com.example.backend_sirhgp.repository.SucursalRepository;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Sucursal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final EmployerRepository employerRepository;
    private final ProjetRepository projetRepository;
    private final EvenementMapper evenementMapper;
    private final EntrepriseContextService entrepriseContextService;
    private final SucursalRepository sucursalRepository;

    @Transactional
    public EvenementResponseDTO create(EvenementRequestDTO dto) {
        Employer createur = getCurrentUser();
        log.info("Création d'événement par utilisateur: {} (ID: {})", createur.getEmail(), createur.getId());

        Evenement event = evenementMapper.toEntity(dto);
        event.setCreateur(createur);
        event.setEntreprise(createur.getEntreprise());
        Sucursal cible = resoudreSuccursale(createur, dto.getSucursalId());
        event.setSucursal(cible);
        if (event.getEntreprise() == null && cible != null) event.setEntreprise(cible.getEntreprise());

        // Participants
        if (dto.getParticipantsIds() != null && !dto.getParticipantsIds().isEmpty()) {
            Set<Employer> participants = dto.getParticipantsIds().stream()
                    .map(pid -> employerRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé : " + pid)))
                    .collect(Collectors.toSet());
            event.setParticipants(participants);
            participants.forEach(p -> verifierAssociation(createur, p.getEntreprise(), p.getSucursal()));
        }

        // Projet
        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            event.setProjet(projet);
            verifierAssociation(createur, projet.getEntreprise(), projet.getSucursal());
            if (event.getSucursal() == null) event.setSucursal(projet.getSucursal());
            else if (projet.getSucursal() != null && !event.getSucursal().getId().equals(projet.getSucursal().getId()))
                throw new org.springframework.security.access.AccessDeniedException("Le projet doit appartenir à la succursale de l'événement.");
        }

        Evenement saved = evenementRepository.save(event);
        log.info("Événement créé : {} (ID: {}) par {}", saved.getTitre(), saved.getId(), createur.getEmail());

        return enrich(evenementMapper.toResponseDTO(saved), saved);
    }

    @Transactional(readOnly = true)
    public Page<EvenementResponseDTO> findAll(Pageable pageable) {
        Employer current = getCurrentUser();
        List<Evenement> scoped = evenementsDansScope(current);
        if (pageable.isUnpaged()) return new PageImpl<>(scoped.stream().map(e -> enrich(evenementMapper.toResponseDTO(e), e)).toList());
        int from = Math.min((int) pageable.getOffset(), scoped.size());
        int to = Math.min(from + pageable.getPageSize(), scoped.size());
        return new PageImpl<>(scoped.subList(from, to).stream().map(e -> enrich(evenementMapper.toResponseDTO(e), e)).toList(), pageable, scoped.size());
    }

    @Transactional(readOnly = true)
    public Page<EvenementResponseDTO> findAll(Pageable pageable, Long requestedEntrepriseId, Long requestedSucursalId) {
        Employer current = getCurrentUser();
        Long entrepriseScope = entrepriseContextService.getScopeEntrepriseId();
        Long sucursalScope = entrepriseContextService.getScopeSucursalId();
        if (requestedEntrepriseId != null && entrepriseScope != null && !entrepriseScope.equals(requestedEntrepriseId))
            throw new org.springframework.security.access.AccessDeniedException("Entreprise hors de votre périmètre.");
        if (requestedSucursalId != null && sucursalScope != null && !sucursalScope.equals(requestedSucursalId))
            throw new org.springframework.security.access.AccessDeniedException("Succursale hors de votre périmètre.");

        List<Evenement> scoped = evenementsDansScope(current).stream()
                .filter(e -> requestedEntrepriseId == null || (e.getEntreprise() != null && requestedEntrepriseId.equals(e.getEntreprise().getId())))
                .filter(e -> requestedSucursalId == null || (e.getSucursal() != null && requestedSucursalId.equals(e.getSucursal().getId())))
                .toList();
        if (pageable.isUnpaged()) return new PageImpl<>(scoped.stream().map(e -> enrich(evenementMapper.toResponseDTO(e), e)).toList());
        int from = Math.min((int) pageable.getOffset(), scoped.size());
        int to = Math.min(from + pageable.getPageSize(), scoped.size());
        return new PageImpl<>(scoped.subList(from, to).stream().map(e -> enrich(evenementMapper.toResponseDTO(e), e)).toList(), pageable, scoped.size());
    }

    @Transactional(readOnly = true)
    public EvenementResponseDTO findById(Long id) {
        Evenement event = evenementRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé : " + id));
        verifierLectureScope(event, getCurrentUser());
        return enrich(evenementMapper.toResponseDTO(event), event);
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByDateRange(LocalDate start, LocalDate end) {
        Employer current = getCurrentUser();
        return evenementDansScopePourPeriode(current, start, end).stream()
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByParticipantId(Long participantId) {
        Employer current = getCurrentUser();
        if (estSecretaire(current)) verifierEmployeDansSuccursale(current, employerRepository.findById(participantId).orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé")));
        List<Evenement> base = evenementsDansScope(current);
        return base.stream().filter(e -> e.getParticipants().stream().anyMatch(p -> participantId.equals(p.getId())))
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByProjetId(Long projetId) {
        Employer current = getCurrentUser();
        Projet projet = projetRepository.findById(projetId).orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé"));
        verifierAssociation(current, projet.getEntreprise(), projet.getSucursal());
        return evenementsDansScope(current).stream().filter(e -> e.getProjet() != null && projetId.equals(e.getProjet().getId()))
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional
    public EvenementResponseDTO update(Long id, EvenementRequestDTO dto) {
        Evenement event = evenementRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé : " + id));

        // ⭐ VÉRIFICATION DES DROITS ⭐
        Employer currentUser = getCurrentUser();
        verifierLectureScope(event, currentUser);
        boolean isAdmin = isAdmin(currentUser);
        boolean isCreator = event.getCreateur().getId().equals(currentUser.getId());

        log.info("Tentative de modification - Événement ID: {}, Utilisateur: {}, isAdmin: {}, isCreator: {}",
                id, currentUser.getEmail(), isAdmin, isCreator);

        if (estSecretaire(currentUser) && !isCreator) throw new org.springframework.security.access.AccessDeniedException("Vous pouvez modifier uniquement vos événements.");
        if (!isAdmin && !isCreator) {
            log.warn("REFUSÉ - Utilisateur {} n'a pas les droits pour modifier l'événement {} (créateur: {})",
                    currentUser.getEmail(), id, event.getCreateur().getEmail());
            throw new SecurityException("Vous n'êtes pas autorisé à modifier cet événement. Seul le créateur peut le modifier.");
        }

        evenementMapper.updateEntity(event, dto);
        if (dto.getSucursalId() != null) event.setSucursal(resoudreSuccursale(currentUser, dto.getSucursalId()));

        if (dto.getParticipantsIds() != null) {
            Set<Employer> participants = dto.getParticipantsIds().stream()
                    .map(pid -> employerRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé : " + pid)))
                    .collect(Collectors.toSet());
            participants.forEach(p -> verifierAssociation(currentUser, p.getEntreprise(), p.getSucursal()));
            event.setParticipants(participants);
        }

        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            event.setProjet(projet);
            verifierAssociation(currentUser, projet.getEntreprise(), projet.getSucursal());
            if (event.getSucursal() == null) event.setSucursal(projet.getSucursal());
            else if (projet.getSucursal() != null && !event.getSucursal().getId().equals(projet.getSucursal().getId()))
                throw new org.springframework.security.access.AccessDeniedException("Le projet doit appartenir à la succursale de l'événement.");
        } else {
            event.setProjet(null);
        }

        Evenement updated = evenementRepository.save(event);
        log.info("Événement modifié : {} (ID: {}) par {}", updated.getTitre(), updated.getId(), currentUser.getEmail());

        return enrich(evenementMapper.toResponseDTO(updated), updated);
    }

    @Transactional
    public void delete(Long id) {
        Evenement event = evenementRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé : " + id));

        // ⭐ VÉRIFICATION DES DROITS ⭐
        Employer currentUser = getCurrentUser();
        verifierLectureScope(event, currentUser);
        boolean isAdmin = isAdmin(currentUser);
        boolean isCreator = event.getCreateur().getId().equals(currentUser.getId());

        log.info("Tentative de suppression - Événement ID: {}, Utilisateur: {}, isAdmin: {}, isCreator: {}",
                id, currentUser.getEmail(), isAdmin, isCreator);

        if (estSecretaire(currentUser)) throw new org.springframework.security.access.AccessDeniedException("La suppression des événements est interdite au rôle SECRETAIRE.");
        if (!isAdmin && !isCreator) {
            log.warn("REFUSÉ - Utilisateur {} n'a pas les droits pour supprimer l'événement {} (créateur: {})",
                    currentUser.getEmail(), id, event.getCreateur().getEmail());
            throw new SecurityException("Vous n'êtes pas autorisé à supprimer cet événement. Seul le créateur peut le supprimer.");
        }

        evenementRepository.deleteById(id);
        log.info("Événement supprimé : {} (ID: {}) par {}", event.getTitre(), id, currentUser.getEmail());
    }

    private boolean estSecretaire(Employer e) { return e.getRole() != null && "SECRETAIRE".equals(e.getRole().getName()); }
    private Sucursal resoudreSuccursale(Employer user, Long sucursalId) {
        if (sucursalId == null) return user.getSucursal();
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée : " + sucursalId));
        verifierAssociation(user, sucursal.getEntreprise(), sucursal);
        return sucursal;
    }
    private List<Evenement> evenementsDansScope(Employer user) {
        Long suc = entrepriseContextService.getScopeSucursalId();
        if (suc != null) return evenementRepository.findBySucursalId(suc);
        Long ent = entrepriseContextService.getScopeEntrepriseId();
        if (ent != null) return evenementRepository.findByEntrepriseId(ent);
        return evenementRepository.findAll();
    }
    private List<Evenement> evenementDansScopePourPeriode(Employer user, LocalDate start, LocalDate end) {
        Long suc = entrepriseContextService.getScopeSucursalId();
        if (suc != null) return evenementRepository.findBySucursalIdAndDateDebutBetween(suc, start, end);
        Long ent = entrepriseContextService.getScopeEntrepriseId();
        if (ent != null) return evenementRepository.findByEntrepriseIdAndDateDebutBetween(ent, start, end);
        return evenementRepository.findByDateDebutBetween(start, end);
    }
    private void verifierLectureScope(Evenement event, Employer user) {
        Long suc = entrepriseContextService.getScopeSucursalId();
        Long ent = entrepriseContextService.getScopeEntrepriseId();
        if ((suc != null && (event.getSucursal() == null || !suc.equals(event.getSucursal().getId())))
                || (ent != null && (event.getEntreprise() == null || !ent.equals(event.getEntreprise().getId()))))
            throw new ResourceNotFoundException("Événement non trouvé : " + event.getId());
    }
    private void verifierAssociation(Employer user, Entreprise entreprise, Sucursal sucursal) {
        Long entScope = entrepriseContextService.getScopeEntrepriseId();
        Long sucScope = entrepriseContextService.getScopeSucursalId();
        if (estSecretaire(user) && (user.getEntreprise() == null || user.getSucursal() == null
                || entreprise == null || sucursal == null
                || !user.getEntreprise().getId().equals(entreprise.getId())
                || !user.getSucursal().getId().equals(sucursal.getId())))
            throw new org.springframework.security.access.AccessDeniedException("Les événements et participants doivent rester dans votre succursale.");
        if (entScope != null && (entreprise == null || !entScope.equals(entreprise.getId())))
            throw new org.springframework.security.access.AccessDeniedException("Élément associé hors entreprise.");
        if (sucScope != null && (sucursal == null || !sucScope.equals(sucursal.getId())))
            throw new org.springframework.security.access.AccessDeniedException("Élément associé hors succursale.");
    }
    private void verifierEmployeDansSuccursale(Employer user, Employer participant) {
        verifierAssociation(user, participant.getEntreprise(), participant.getSucursal());
    }

    // ─── Méthodes utilitaires ─────────────────────────────────────────

    private EvenementResponseDTO enrich(EvenementResponseDTO dto, Evenement event) {
        // ⭐ AJOUTER L'ID ET LE NOM DU CRÉATEUR ⭐
        if (event.getCreateur() != null) {
            dto.setCreateurId(event.getCreateur().getId());
            dto.setCreateurNom(event.getCreateur().getName());
        }

        LocalDate aujourd = LocalDate.now();
        dto.setEstPasse(event.getDateDebut().isBefore(aujourd));
        dto.setEstEnCours(!event.getDateDebut().isAfter(aujourd) &&
                (event.getDateFin() == null || !event.getDateFin().isBefore(aujourd)));
        dto.setEstAvenir(event.getDateDebut().isAfter(aujourd));
        dto.setNombreParticipants((long) event.getParticipants().size());

        if (event.getDateDebut() != null && event.getDateFin() != null) {
            LocalDateTime debut = event.getDateDebut().atTime(
                    event.getHeureDebut() != null ? event.getHeureDebut() : LocalTime.MIDNIGHT);
            LocalDateTime fin = event.getDateFin().atTime(
                    event.getHeureFin() != null ? event.getHeureFin() : LocalTime.MIDNIGHT);
            dto.setDureeMinutes(ChronoUnit.MINUTES.between(debut, fin));
        } else {
            dto.setDureeMinutes(0L);
        }
        return dto;
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé: " + auth.getName()));
    }

    private boolean isAdmin(Employer user) {
        if (user == null || user.getRole() == null) return false;
        String roleName = user.getRole().getName();
        return "SUPER_ADMIN".equals(roleName) || "ADMIN".equals(roleName);
    }
}
