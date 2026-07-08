package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.FactureDispatchRequestDTO;
import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.request.LigneFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.event.FactureDispatchedEvent;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.exception.UnauthorizedException;
import com.example.backend_sirhgp.mapper.FactureMapper;
import com.example.backend_sirhgp.mapper.LigneFactureMapper;
import com.example.backend_sirhgp.model.*;
import com.example.backend_sirhgp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FactureService {

    private final FactureRepository factureRepository;
    private final ProjetRepository projetRepository;
    private final EmployerRepository employerRepository;
    private final TacheRepository tacheRepository;
    private final FactureMapper factureMapper;
    private final LigneFactureMapper ligneFactureMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─── CRUD de base ─────────────────────────────────────────────────────────

    @Transactional
    public FactureResponseDTO create(FactureRequestDTO dto) {
        Facture facture = factureMapper.toEntity(dto);
        Employer emetteur = getCurrentUser();
        facture.setEmetteur(emetteur);

        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            facture.setProjet(projet);
        }

        if (dto.getClientId() != null) {
            Employer client = employerRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé : " + dto.getClientId()));
            facture.setClient(client);
            facture.setDateEnvoi(LocalDateTime.now());
        }

        if (dto.getLignesFacture() != null && !dto.getLignesFacture().isEmpty()) {
            for (LigneFactureRequestDTO ligneDto : dto.getLignesFacture()) {
                LigneFacture ligne = ligneFactureMapper.toEntity(ligneDto);
                if (ligneDto.getTacheId() != null) {
                    Tache tache = tacheRepository.findById(ligneDto.getTacheId())
                            .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + ligneDto.getTacheId()));
                    ligne.setTache(tache);
                }
                facture.addLigneFacture(ligne);
            }
        }

        facture.calculerMontants();
        Facture saved = factureRepository.save(facture);
        updateBudgetProjet(saved);

        log.info("Facture créée : {} - {}", saved.getNumeroFacture(), saved.getTitre());
        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    @Transactional(readOnly = true)
    public Page<FactureResponseDTO> findAll(Pageable pageable) {
        return factureRepository.findAll(pageable)
                .map(f -> enrich(factureMapper.toResponseDTO(f), f));
    }

    @Transactional(readOnly = true)
    public FactureResponseDTO findById(Long id) {
        Facture facture = factureRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));
        return enrich(factureMapper.toResponseDTO(facture), facture);
    }

    @Transactional(readOnly = true)
    public List<FactureResponseDTO> findByProjetId(Long projetId) {
        return factureRepository.findByProjetId(projetId).stream()
                .map(f -> enrich(factureMapper.toResponseDTO(f), f))
                .collect(Collectors.toList());
    }

    @Transactional
    public FactureResponseDTO update(Long id, FactureRequestDTO dto) {
        Facture facture = factureRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));
        factureMapper.updateEntity(facture, dto);
        facture.calculerMontants();
        Facture saved = factureRepository.save(facture);
        updateBudgetProjet(saved);
        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    @Transactional
    public FactureResponseDTO updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));
        facture.setStatut(statut);
        facture.calculerMontants();
        return enrich(factureMapper.toResponseDTO(factureRepository.save(facture)), facture);
    }

    @Transactional
    public FactureResponseDTO enregistrerPaiement(Long id, BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être positif");
        }
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));

        facture.setMontantPaye(facture.getMontantPaye().add(montant));
        facture.calculerMontants();
        facture = factureRepository.save(facture);
        updateBudgetProjet(facture);
        return enrich(factureMapper.toResponseDTO(facture), facture);
    }

    @Transactional
    public void delete(Long id) {
        if (!factureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Facture non trouvée : " + id);
        }
        factureRepository.deleteById(id);
        log.info("Facture supprimée : {}", id);
    }

    // ─── DISPATCH (ENVOI AU CLIENT) ───────────────────────────────────────────

    @Transactional
    public FactureResponseDTO dispatchToClient(Long factureId, FactureDispatchRequestDTO dto) {
        Employer emetteur = getCurrentUser();

        Facture facture = factureRepository.findByIdWithRelations(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée: " + factureId));

        // Vérifier que l'utilisateur est bien l'émetteur
        if (!facture.getEmetteur().getId().equals(emetteur.getId())) {
            throw new UnauthorizedException("Seul l'émetteur peut envoyer cette facture");
        }

        Employer client = employerRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé: " + dto.getClientId()));

        // Vérifier qu'on n'envoie pas au Super Admin
        if (isSuperAdmin(client)) {
            throw new IllegalArgumentException("Impossible d'envoyer une facture au Super Administrateur");
        }

        // Mettre à jour les infos de dispatch
        facture.setClient(client);
        facture.setMessage(dto.getMessage());
        facture.setDateEnvoi(LocalDateTime.now());
        facture.setEstConsultee(false);

        Facture saved = factureRepository.save(facture);

        // Publier l'événement de dispatch
        eventPublisher.publishEvent(new FactureDispatchedEvent(
                this, saved, emetteur, client, dto.getMessage()));

        log.info("📨 Facture {} envoyée à {}", saved.getNumeroFacture(), client.getEmail());

        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    @Transactional
    public void markAsConsulted(Long factureId) {
        Employer currentUser = getCurrentUser();

        Facture facture = factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée: " + factureId));

        // Seul le client peut marquer comme consultée
        if (facture.getClient() == null || !facture.getClient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le destinataire de cette facture");
        }

        facture.marquerCommeConsultee();
        factureRepository.save(facture);

        log.info("Facture {} marquée comme consultée par {}", factureId, currentUser.getEmail());
    }

    @Transactional(readOnly = true)
    public List<FactureResponseDTO> getReceivedFactures() {
        Employer currentUser = getCurrentUser();
        return factureRepository.findByClientId(currentUser.getId())
                .stream()
                .map(f -> enrich(factureMapper.toResponseDTO(f), f))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FactureResponseDTO> getUnconsultedFactures() {
        Employer currentUser = getCurrentUser();
        return factureRepository.findByClientIdAndEstConsulteeFalse(currentUser.getId())
                .stream()
                .map(f -> enrich(factureMapper.toResponseDTO(f), f))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FactureResponseDTO> getSentFactures() {
        Employer currentUser = getCurrentUser();
        return factureRepository.findByEmetteurId(currentUser.getId())
                .stream()
                .map(f -> enrich(factureMapper.toResponseDTO(f), f))
                .collect(Collectors.toList());
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private FactureResponseDTO enrich(FactureResponseDTO dto, Facture facture) {
        if (facture.getMontantTTC() != null
                && facture.getMontantTTC().compareTo(BigDecimal.ZERO) > 0
                && facture.getMontantPaye() != null) {
            double pct = facture.getMontantPaye()
                    .divide(facture.getMontantTTC(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            dto.setPourcentagePaye(pct);
        } else {
            dto.setPourcentagePaye(0.0);
        }

        dto.setEstPayee(facture.getStatut() == Facture.StatutFacture.PAYEE);
        dto.setEstEnRetard(facture.getStatut() == Facture.StatutFacture.EN_RETARD);

        if (facture.getDateEcheance() != null) {
            dto.setJoursRestants(ChronoUnit.DAYS.between(LocalDate.now(), facture.getDateEcheance()));
        }
        if (facture.getDateEmission() != null) {
            dto.setDelaiEmissionPaiement(ChronoUnit.DAYS.between(facture.getDateEmission(), LocalDate.now()));
        }
        return dto;
    }

    private void updateBudgetProjet(Facture facture) {
        if (facture.getProjet() == null) return;
        Long projetId = facture.getProjet().getId();
        BigDecimal total = factureRepository.sumMontantTTCByProjetId(projetId);
        Projet projet = facture.getProjet();
        projet.setBudgetConsomme(total);
        projet.updateBudget();
        projetRepository.save(projet);
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }

    private boolean isSuperAdmin(Employer employer) {
        return employer.getRole() != null &&
                "SUPER_ADMIN".equalsIgnoreCase(employer.getRole().getName());
    }
}