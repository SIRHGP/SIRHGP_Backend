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

/**
 * CORRECTIONS :
 * - EvenementRequestDTO / EvenementResponseDTO au lieu de l'ancien EvenementDTO.
 * - createurId extrait du JWT — pas du body client.
 * - findByParticipantsContaining(Employer) → findByParticipantId(Long).
 * - findAll() paginé.
 * - findByIdWithRelations() pour éviter N+1.
 */
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
        // CORRECTION : créateur = utilisateur connecté
        Employer createur = getCurrentUser();

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
        log.info("Événement créé : {} par {}", saved.getTitre(), createur.getEmail());
        return enrich(evenementMapper.toResponseDTO(saved), saved);
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<EvenementResponseDTO> findAll(Pageable pageable) {
        return evenementRepository.findAll(pageable)
                .map(e -> enrich(evenementMapper.toResponseDTO(e), e));
    }

    @Transactional(readOnly = true)
    public EvenementResponseDTO findById(Long id) {
        // CORRECTION : findByIdWithRelations pour éviter N+1
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
        // CORRECTION : findByParticipantId(Long) au lieu de findByParticipantsContaining(Employer)
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
        return enrich(evenementMapper.toResponseDTO(updated), updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!evenementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Événement non trouvé : " + id);
        }
        evenementRepository.deleteById(id);
        log.info("Événement supprimé : {}", id);
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private EvenementResponseDTO enrich(EvenementResponseDTO dto, Evenement event) {
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
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }
}