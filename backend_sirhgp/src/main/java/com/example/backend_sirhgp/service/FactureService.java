package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.FactureDispatchRequestDTO;
import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.request.LigneFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.dto.response.PaiementFactureResponseDTO;
import com.example.backend_sirhgp.dto.request.PaiementFactureRequestDTO;
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
    private final PaiementFactureRepository paiementFactureRepository;
    private final FacturePdfService facturePdfService;
    private final EntrepriseContextService entrepriseContextService;

    // ─── CRUD de base ─────────────────────────────────────────────────────────

    @Transactional
    public FactureResponseDTO create(FactureRequestDTO dto) {
        Facture facture = factureMapper.toEntity(dto);
        Employer emetteur = getCurrentUser();
        Long entrepriseId = requireEntrepriseId(emetteur);
        if (entrepriseId == null) {
            throw new UnauthorizedException("Une entreprise est requise pour créer une facture");
        }
        facture.setEmetteur(emetteur);
        facture.setEntreprise(emetteur.getEntreprise());

        if (dto.getProjetId() != null) {
            Projet projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            verifierEntreprise(projet.getEntreprise() != null ? projet.getEntreprise().getId() : null, entrepriseId,
                    "Projet non trouvé : " + dto.getProjetId());
            facture.setProjet(projet);
        }

        if (dto.getClientId() != null) {
            Employer client = employerRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé : " + dto.getClientId()));
            facture.setClient(client);
            facture.setDateEnvoi(LocalDateTime.now());
            facture.setNomDestinataire(client.getName());
        }

        appliquerLignes(facture, dto.getLignesFacture());

        facture.calculerMontants();
        Facture saved = factureRepository.save(facture);
        updateBudgetProjet(saved);

        log.info("Facture créée : {} - {}", saved.getNumeroFacture(), saved.getTitre());
        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    @Transactional(readOnly = true)
    public Page<FactureResponseDTO> findAll(Pageable pageable) {
        Employer current = getCurrentUser();
        Long entrepriseId = requireEntrepriseId(current);
        Page<Facture> factures = entrepriseId == null
                ? factureRepository.findAll(pageable)
                : factureRepository.findByEntrepriseId(entrepriseId, pageable);
        return factures
                .map(f -> enrich(factureMapper.toResponseDTO(f), f));
    }

    @Transactional(readOnly = true)
    public FactureResponseDTO findById(Long id) {
        Facture facture = requireFactureInScope(id, true);
        return enrich(factureMapper.toResponseDTO(facture), facture);
    }

    @Transactional(readOnly = true)
    public List<FactureResponseDTO> findByProjetId(Long projetId) {
        Employer current = getCurrentUser();
        Long entrepriseId = requireEntrepriseId(current);
        List<Facture> factures = entrepriseId == null
                ? factureRepository.findByProjetId(projetId)
                : factureRepository.findByProjetIdAndEntrepriseId(projetId, entrepriseId);
        return factures.stream()
                .map(f -> enrich(factureMapper.toResponseDTO(f), f))
                .collect(Collectors.toList());
    }

    @Transactional
    public FactureResponseDTO update(Long id, FactureRequestDTO dto) {
        Facture facture = requireFactureInScope(id, true);
        Long entrepriseId = requireEntrepriseId(getCurrentUser());
        Projet projet = null;
        if (dto.getProjetId() != null) {
            projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            verifierEntreprise(projet.getEntreprise() != null ? projet.getEntreprise().getId() : null,
                    entrepriseId, "Projet non trouvé : " + dto.getProjetId());
        }
        Projet ancienProjet = facture.getProjet();
        factureMapper.updateEntity(facture, dto);
        facture.setProjet(projet);
        appliquerLignes(facture, dto.getLignesFacture());
        facture.calculerMontants();
        Facture saved = factureRepository.save(facture);
        if (ancienProjet != null && (projet == null || !ancienProjet.getId().equals(projet.getId()))) {
            updateBudgetProjet(ancienProjet);
        }
        updateBudgetProjet(saved);
        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    private void appliquerLignes(Facture facture, List<LigneFactureRequestDTO> lignesDto) {
        if (lignesDto == null) return;
        facture.getLignesFacture().clear();
        for (LigneFactureRequestDTO ligneDto : lignesDto) {
            if (ligneDto == null || ligneDto.getDescription() == null || ligneDto.getDescription().isBlank()
                    || ligneDto.getQuantite() == null || ligneDto.getQuantite() < 1
                    || ligneDto.getPrixUnitaire() == null || ligneDto.getPrixUnitaire().signum() < 0) {
                throw new IllegalArgumentException("Toutes les lignes doivent contenir une désignation, une quantité valide et un prix.");
            }
            LigneFacture ligne = ligneFactureMapper.toEntity(ligneDto);
            if (ligneDto.getTacheId() != null) {
                Tache tache = tacheRepository.findById(ligneDto.getTacheId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + ligneDto.getTacheId()));
                ligne.setTache(tache);
            }
            facture.addLigneFacture(ligne);
        }
    }

    @Transactional
    public FactureResponseDTO updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = requireFactureInScope(id, false);
        facture.setStatut(statut);
        facture.calculerMontants();
        return enrich(factureMapper.toResponseDTO(factureRepository.save(facture)), facture);
    }

    @Transactional
    public FactureResponseDTO enregistrerPaiement(Long id, BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être positif");
        }
        Facture facture = requireFactureInScope(id, false);

        facture.setMontantPaye(facture.getMontantPaye().add(montant));
        facture.calculerMontants();
        facture = factureRepository.save(facture);
        updateBudgetProjet(facture);
        return enrich(factureMapper.toResponseDTO(facture), facture);
    }

    @Transactional
    public FactureResponseDTO enregistrerPaiement(Long id, PaiementFactureRequestDTO dto) {
        FactureResponseDTO response = enregistrerPaiement(id, dto.getMontant());
        Facture facture = requireFactureInScope(id, false);
        Employer user = getCurrentUser();
        PaiementFacture paiement = PaiementFacture.builder()
                .facture(facture)
                .datePaiement(dto.getDatePaiement() != null ? dto.getDatePaiement() : LocalDate.now())
                .mode(dto.getMode())
                .montant(dto.getMontant())
                .reference(dto.getReference())
                .note(dto.getNote())
                .compteComptable(dto.getMode() != null ? dto.getMode().getCompteComptable() : null)
                .enregistrePar(user)
                .build();
        paiementFactureRepository.save(paiement);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PaiementFactureResponseDTO> getPaiements(Long factureId) {
        requireFactureInScope(factureId, false);
        return paiementFactureRepository.findByFactureIdOrderByDatePaiementDesc(factureId).stream().map(p -> {
            PaiementFactureResponseDTO dto = new PaiementFactureResponseDTO();
            dto.setId(p.getId());
            dto.setFactureId(factureId);
            dto.setMode(p.getMode());
            dto.setMontant(p.getMontant());
            dto.setDatePaiement(p.getDatePaiement());
            dto.setReference(p.getReference());
            dto.setNote(p.getNote());
            dto.setCompteComptable(p.getCompteComptable());
            if (p.getEnregistrePar() != null) {
                dto.setEnregistreParId(p.getEnregistrePar().getId());
                dto.setEnregistreParNom(p.getEnregistrePar().getName());
            }
            dto.setDateCreation(p.getDateCreation());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(Long factureId) {
        Facture facture = requireFactureInScope(factureId, true);
        return facturePdfService.generer(facture);
    }

    @Transactional
    public void delete(Long id) {
        Facture facture = requireFactureInScope(id, false);
        factureRepository.delete(facture);
        log.info("Facture supprimée : {}", id);
    }

    // ─── DISPATCH (ENVOI AU CLIENT) ───────────────────────────────────────────

    @Transactional
    public FactureResponseDTO dispatchToClient(Long factureId, FactureDispatchRequestDTO dto) {
        Employer emetteur = getCurrentUser();

        Facture facture = requireFactureInScope(factureId, true);

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
        return factureRepository.findByClientId(currentUser.getId()).stream()
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
        requireEntrepriseId(currentUser);
        Long entrepriseId = currentUser.getEntreprise().getId();
        return factureRepository.findByEmetteurId(currentUser.getId())
                .stream().filter(f -> f.getEntreprise() != null
                        && entrepriseId.equals(f.getEntreprise().getId()))
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
        if (facture.getProjet() != null) updateBudgetProjet(facture.getProjet());
    }

    private void updateBudgetProjet(Projet projet) {
        if (projet == null) return;
        Long projetId = projet.getId();
        Long entrepriseId = projet.getEntreprise() != null ? projet.getEntreprise().getId() : null;
        BigDecimal total = entrepriseId == null ? factureRepository.sumMontantTTCByProjetId(projetId)
                : factureRepository.sumMontantTTCByProjetIdAndEntrepriseId(projetId, entrepriseId);
        projet.setBudgetConsomme(total);
        projet.updateBudget();
        projetRepository.save(projet);
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }

    private Long requireEntrepriseId(Employer user) {
        if (isSuperAdmin(user)) return null;
        if (user.getEntreprise() == null || user.getEntreprise().getId() == null) {
            throw new UnauthorizedException("Votre compte n'est rattaché à aucune entreprise");
        }
        return user.getEntreprise().getId();
    }

    private Facture requireFactureInScope(Long factureId, boolean loadRelations) {
        Employer current = getCurrentUser();
        Long entrepriseId = requireEntrepriseId(current);
        if (entrepriseId == null) {
            return (loadRelations ? factureRepository.findByIdWithRelations(factureId)
                    : factureRepository.findById(factureId))
                    .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + factureId));
        }
        return factureRepository.findByIdAndEntrepriseId(factureId, entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + factureId));
    }

    private void verifierEntreprise(Long ressourceEntrepriseId, Long entrepriseId, String message) {
        if (entrepriseId != null && !entrepriseId.equals(ressourceEntrepriseId)) {
            throw new ResourceNotFoundException(message);
        }
    }

    private boolean isSuperAdmin(Employer employer) {
        return employer.getRole() != null &&
                "SUPER_ADMIN".equalsIgnoreCase(employer.getRole().getName());
    }
}
