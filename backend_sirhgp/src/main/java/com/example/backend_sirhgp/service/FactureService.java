package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.request.LigneFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.FactureMapper;
import com.example.backend_sirhgp.mapper.LigneFactureMapper;
import com.example.backend_sirhgp.model.*;
import com.example.backend_sirhgp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORRECTIONS :
 * - FactureRequestDTO / FactureResponseDTO au lieu de l'ancien FactureDTO.
 * - enregistrerPaiement() utilise BigDecimal.add() — pas Double +.
 * - enrichFactureDTO() utilise BigDecimal.compareTo() — pas > sur Double.
 * - emetteurId extrait du JWT (utilisateur connecté), pas du body.
 * - sumMontantFacturesByProjet() → sumMontantTTCByProjetId() (renommé dans repo).
 * - findAll() paginé.
 * - findByIdWithRelations() pour éviter N+1.
 */
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

    @Transactional
    public FactureResponseDTO create(FactureRequestDTO dto) {
        Facture facture = factureMapper.toEntity(dto);

        // CORRECTION : émetteur = utilisateur connecté
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
        }

        // Lignes de facture
        if (dto.getLignesFacture() != null && !dto.getLignesFacture().isEmpty()) {
            for (LigneFactureRequestDTO ligneDto : dto.getLignesFacture()) {
                LigneFacture ligne = ligneFactureMapper.toEntity(ligneDto);
                if (ligneDto.getTacheId() != null) {
                    Tache tache = tacheRepository.findById(ligneDto.getTacheId())
                            .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + ligneDto.getTacheId()));
                    ligne.setTache(tache);
                }
                // Utilise la méthode métier qui appelle calculerMontants()
                facture.addLigneFacture(ligne);
            }
        }

        facture.calculerMontants();
        Facture saved = factureRepository.save(facture);

        // Mise à jour du budget consommé du projet
        updateBudgetProjet(saved);

        log.info("Facture créée : {} - {}", saved.getNumeroFacture(), saved.getTitre());
        return enrich(factureMapper.toResponseDTO(saved), saved);
    }

    // CORRECTION : pagination
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
        return enrich(factureMapper.toResponseDTO(factureRepository.save(facture)), facture);
    }

    @Transactional
    public FactureResponseDTO updateStatut(Long id, Facture.StatutFacture statut) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));
        facture.setStatut(statut);
        facture.calculerMontants();
        return enrich(factureMapper.toResponseDTO(factureRepository.save(facture)), facture);
    }

    /**
     * CORRECTION : BigDecimal.add() au lieu de Double +
     */
    @Transactional
    public FactureResponseDTO enregistrerPaiement(Long id, BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être positif");
        }
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture non trouvée : " + id));

        // CORRECTION : BigDecimal.add() — jamais Double +
        facture.setMontantPaye(facture.getMontantPaye().add(montant));
        facture.calculerMontants();
        facture = factureRepository.save(facture);
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

    // ─── Utilitaires ─────────────────────────────────────────

    private FactureResponseDTO enrich(FactureResponseDTO dto, Facture facture) {
        // CORRECTION : BigDecimal.compareTo() au lieu de > sur Double
        if (facture.getMontantTTC() != null
                && facture.getMontantTTC().compareTo(BigDecimal.ZERO) > 0
                && facture.getMontantPaye() != null) {
            double pct = facture.getMontantPaye()
                    .divide(facture.getMontantTTC(), 4, java.math.RoundingMode.HALF_UP)
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
        // CORRECTION : sumMontantTTCByProjetId() (renommé dans le repo corrigé)
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
}