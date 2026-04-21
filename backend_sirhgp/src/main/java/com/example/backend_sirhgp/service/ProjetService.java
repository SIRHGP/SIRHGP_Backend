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

    @Transactional
    public ProjetResponseDTO create(ProjetRequestDTO dto) {
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
        projet.updateBudget();
        Projet saved = projetRepository.save(projet);
        log.info("Projet créé : {} ({})", saved.getNom(), saved.getCodeProjet());
        return enrichAndMap(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProjetResponseDTO> findAll(Pageable pageable) {
        return projetRepository.findAll(pageable).map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public ProjetResponseDTO findById(Long id) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        return enrichAndMap(projet);
    }

    @Transactional
    public ProjetResponseDTO update(Long id, ProjetRequestDTO dto) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        projetMapper.updateEntity(projet, dto);
        resolveRelations(projet, dto);
        if (projet.getStatut() == Projet.StatutProjet.TERMINE && projet.getDateFinReelle() == null) {
            projet.setDateFinReelle(LocalDate.now());
        }
        projet.updateBudget();
        projet.verifierEtMettreAJourRetard();
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public void delete(Long id) {
        if (!projetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Projet non trouvé : " + id);
        }
        projetRepository.deleteById(id);
        log.info("Projet supprimé : {}", id);
    }

    @Transactional
    public ProjetResponseDTO updateStatut(Long id, Projet.StatutProjet statut) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
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
        Employer membre = employerRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé : " + membreId));
        projet.addMembre(membre);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO removeMembre(Long projetId, Long membreId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        Employer membre = employerRepository.findById(membreId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé : " + membreId));
        projet.removeMembre(membre);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO addDepartement(Long projetId, Long deptId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        Departement dept = departementRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + deptId));
        projet.addDepartement(dept);
        return enrichAndMap(projetRepository.save(projet));
    }

    @Transactional
    public ProjetResponseDTO removeDepartement(Long projetId, Long deptId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + projetId));
        Departement dept = departementRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + deptId));
        projet.removeDepartement(dept);
        return enrichAndMap(projetRepository.save(projet));
    }

    // CORRECTION : méthode manquante appelée depuis ProjetController
    public List<ProjetResponseDTO> findByDepartement(Long departementId) {
        return projetRepository.findByDepartementId(departementId).stream()
                .map(this::enrichAndMap)
                .collect(Collectors.toList());
    }

    // CORRECTION : méthode manquante appelée depuis ProjetController
    public List<ProjetResponseDTO> findProjetsEnRetard() {
        return projetRepository.findByDateFinPrevueBeforeAndStatutNot(
                        LocalDate.now(), Projet.StatutProjet.TERMINE)
                .stream().map(this::enrichAndMap).collect(Collectors.toList());
    }

    // ─── Dashboard ────────────────────────────────────────────

    // CORRECTION : méthodes manquantes appelées depuis ProjetController
    @Transactional(readOnly = true)
    public ProjetDashboardDTO getGlobalDashboardStats() {
        return ProjetDashboardDTO.builder()
                .totalProjets((int) projetRepository.count())
                .projetsEnCours((int) projetRepository.countByStatut(Projet.StatutProjet.EN_COURS))
                .projetsTermines((int) projetRepository.countByStatut(Projet.StatutProjet.TERMINE))
                .projetsEnRetard((int) projetRepository.countByStatut(Projet.StatutProjet.EN_RETARD))
                .projetsPlanifies((int) projetRepository.countByStatut(Projet.StatutProjet.PLANIFIE))
                .projetsSuspendus((int) projetRepository.countByStatut(Projet.StatutProjet.SUSPENDU))
                .projetsAnnules((int) projetRepository.countByStatut(Projet.StatutProjet.ANNULE))
                .budgetTotalAlloue(projetRepository.sumBudgetAlloue())
                .budgetTotalConsomme(projetRepository.sumBudgetConsomme())
                .totalTaches((int) projetRepository.countTotalTaches())
                .tachesTerminees((int) projetRepository.countTachesByStatut(Tache.StatutTache.TERMINEE))
                .tachesEnCours((int) projetRepository.countTachesByStatut(Tache.StatutTache.EN_COURS))
                .build();
    }

    public ProjetDashboardDTO.ProjetDetailView getProjetDashboardStats(Long id) {
        Projet projet = projetRepository.findByIdWithMembres(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + id));
        return ProjetDashboardDTO.ProjetDetailView.builder()
                .id(projet.getId())
                .nom(projet.getNom())
                .codeProjet(projet.getCodeProjet())
                .statut(projet.getStatut())
                .dateDebut(projet.getDateDebut())
                .dateFinPrevue(projet.getDateFinPrevue())
                .progression(projet.getProgression())
                .totalTaches((int) tacheRepository.countByProjetId(id))
                .tachesTerminees((int) tacheRepository.countByProjetIdAndStatut(id, Tache.StatutTache.TERMINEE))
                .tachesEnCours((int) tacheRepository.countByProjetIdAndStatut(id, Tache.StatutTache.EN_COURS))
                .nombreMembres(projet.getMembres().size())
                .chefProjetId(projet.getChefProjet() != null ? projet.getChefProjet().getId() : null)
                .chefProjetNom(projet.getChefProjet() != null ? projet.getChefProjet().getName() : null)
                .build();
    }

    public List<ProjetDashboardDTO.StatutStatDTO> getStatistiquesParStatut() {
        long total = projetRepository.count();
        return Arrays.stream(Projet.StatutProjet.values()).map(statut -> {
            long count = projetRepository.countByStatut(statut);
            return ProjetDashboardDTO.StatutStatDTO.builder()
                    .statut(statut)
                    .libelle(statut.name())
                    .count((int) count)
                    .pourcentage(total > 0 ? (count * 100.0 / total) : 0.0)
                    .build();
        }).collect(Collectors.toList());
    }

    public ProjetDashboardDTO.BudgetGlobalDTO getStatistiquesBudget() {
        var alloue  = projetRepository.sumBudgetAlloue();
        var consomme = projetRepository.sumBudgetConsomme();
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
        Long id = projet.getId();
        dto.setNombreTaches(tacheRepository.countByProjetId(id));
        dto.setNombreTachesTerminees(tacheRepository.countByProjetIdAndStatut(id, Tache.StatutTache.TERMINEE));
        dto.setNombreTachesEnCours(tacheRepository.countByProjetIdAndStatut(id, Tache.StatutTache.EN_COURS));
        dto.setNombreTachesEnRetard(tacheRepository.countTachesEnRetardByProjetId(id, LocalDate.now(), Tache.StatutTache.TERMINEE));
        dto.setNombreFactures(factureRepository.countByProjetId(id));
        dto.setMontantTotalFactures(factureRepository.sumMontantTTCByProjetId(id));
        dto.setMontantPayeFactures(factureRepository.sumMontantPayeByProjetId(id));
        dto.setNombreMembres((long) projet.getMembres().size());
        dto.setNombreDepartements((long) projet.getDepartements().size());
        if (projet.getDateFinPrevue() != null) {
            long jours = ChronoUnit.DAYS.between(LocalDate.now(), projet.getDateFinPrevue());
            dto.setJoursRestants(jours);
            dto.setEstEnRetard(jours < 0 && projet.getStatut() != Projet.StatutProjet.TERMINE);
        }
        return dto;
    }
}