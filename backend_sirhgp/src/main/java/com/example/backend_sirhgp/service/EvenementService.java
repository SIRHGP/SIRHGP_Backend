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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public EvenementResponseDTO create(EvenementRequestDTO dto) {
        Employer createur = getCurrentUser();
        log.info("Création d'événement par utilisateur: {} (ID: {})", createur.getEmail(), createur.getId());

        Evenement event = evenementMapper.toEntity(dto);
        event.setCreateur(createur);

        // Participants
        if (dto.getParticipantsIds() != null && !dto.getParticipantsIds().isEmpty()) {
            Set<Employer> participants = dto.getParticipantsIds().stream()
                    .map(pid -> employerRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé : " + pid)))
                    .collect(Collectors.toSet());
            event.setParticipants(participants);
        }

        // Projet
        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            event.setProjet(projet);
        }

        Evenement saved = evenementRepository.save(event);
        log.info("Événement créé : {} (ID: {}) par {}", saved.getTitre(), saved.getId(), createur.getEmail());

        return enrich(evenementMapper.toResponseDTO(saved), saved);
    }

    @Transactional(readOnly = true)
    public Page<EvenementResponseDTO> findAll(Pageable pageable) {
        return evenementRepository.findAll(pageable)
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e));
    }

    @Transactional(readOnly = true)
    public EvenementResponseDTO findById(Long id) {
        Evenement event = evenementRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé : " + id));
        return enrich(evenementMapper.toResponseDTO(event), event);
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByDateRange(LocalDate start, LocalDate end) {
        return evenementRepository.findByDateDebutBetween(start, end).stream()
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByParticipantId(Long participantId) {
        return evenementRepository.findByParticipantId(participantId).stream()
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvenementResponseDTO> findByProjetId(Long projetId) {
        return evenementRepository.findByProjetId(projetId).stream()
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e))
                .collect(Collectors.toList());
    }

    @Transactional
    public EvenementResponseDTO update(Long id, EvenementRequestDTO dto) {
        Evenement event = evenementRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé : " + id));

        // ⭐ VÉRIFICATION DES DROITS ⭐
        Employer currentUser = getCurrentUser();
        boolean isAdmin = isAdmin(currentUser);
        boolean isCreator = event.getCreateur().getId().equals(currentUser.getId());

        log.info("Tentative de modification - Événement ID: {}, Utilisateur: {}, isAdmin: {}, isCreator: {}",
                id, currentUser.getEmail(), isAdmin, isCreator);

        if (!isAdmin && !isCreator) {
            log.warn("REFUSÉ - Utilisateur {} n'a pas les droits pour modifier l'événement {} (créateur: {})",
                    currentUser.getEmail(), id, event.getCreateur().getEmail());
            throw new SecurityException("Vous n'êtes pas autorisé à modifier cet événement. Seul le créateur peut le modifier.");
        }

        evenementMapper.updateEntity(event, dto);

        if (dto.getParticipantsIds() != null) {
            Set<Employer> participants = dto.getParticipantsIds().stream()
                    .map(pid -> employerRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Participant non trouvé : " + pid)))
                    .collect(Collectors.toSet());
            event.setParticipants(participants);
        }

        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            event.setProjet(projet);
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
        boolean isAdmin = isAdmin(currentUser);
        boolean isCreator = event.getCreateur().getId().equals(currentUser.getId());

        log.info("Tentative de suppression - Événement ID: {}, Utilisateur: {}, isAdmin: {}, isCreator: {}",
                id, currentUser.getEmail(), isAdmin, isCreator);

        if (!isAdmin && !isCreator) {
            log.warn("REFUSÉ - Utilisateur {} n'a pas les droits pour supprimer l'événement {} (créateur: {})",
                    currentUser.getEmail(), id, event.getCreateur().getEmail());
            throw new SecurityException("Vous n'êtes pas autorisé à supprimer cet événement. Seul le créateur peut le supprimer.");
        }

        evenementRepository.deleteById(id);
        log.info("Événement supprimé : {} (ID: {}) par {}", event.getTitre(), id, currentUser.getEmail());
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