package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.TacheRequestDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.event.TaskAssignedEvent;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.TacheMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.repository.CommentaireTacheRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

        Projet projet = projetRepository.findById(dto.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
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
        return tacheRepository.findAll(pageable)
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t));
    }

    @Transactional(readOnly = true)
    public TacheResponseDTO findById(Long id) {
        // CORRECTION : findByIdWithRelations pour éviter N+1
        Tache tache = tacheRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));
        return enrich(tacheMapper.toResponseDTO(tache), tache);
    }

    @Transactional(readOnly = true)
    public List<TacheResponseDTO> findByAssigneeId(Long assigneeId) {
        return tacheRepository.findByProjetIdWithAssignee(assigneeId).stream()
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponseDTO> findByProjetId(Long projetId) {
        // CORRECTION : query avec JOIN FETCH sur assignee
        return tacheRepository.findByProjetIdWithAssignee(projetId).stream()
                .map(t -> enrich(tacheMapper.toResponseDTO(t), t))
                .collect(Collectors.toList());
    }

    @Transactional
    public TacheResponseDTO update(Long id, TacheRequestDTO dto) {
        Tache tache = tacheRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + id));

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
        tache.setStatut(statut);
        updateDatesStatut(tache);
        return enrich(tacheMapper.toResponseDTO(tacheRepository.save(tache)), tache);
    }

    @Transactional
    public void delete(Long id) {
        if (!tacheRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tâche non trouvée : " + id);
        }
        tacheRepository.deleteById(id);
        log.info("Tâche supprimée : {}", id);
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