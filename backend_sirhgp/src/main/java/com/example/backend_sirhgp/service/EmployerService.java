package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.request.EmployerUpdateDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.dto.response.EmployerStatsDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.EmployerMapper;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * CORRECTIONS :
 * - EmployerCreateDTO / EmployerUpdateDTO / EmployerResponseDTO au lieu de EmployerDTO.
 * - motDePasse RETIRÉ de updateEmployer() — endpoint dédié changePassword().
 * - findByIdWithDetails() utilisé pour charger poste + département sans N+1.
 * - findAll() paginé.
 * - Stats séparées dans getStats() — pas chargées à chaque getById().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final TacheRepository tacheRepository;
    private final EvenementRepository evenementRepository;
    private final DocumentRepository documentRepository;
    private final FactureRepository factureRepository;
    private final EmployerMapper employerMapper;

    @Transactional
    public EmployerResponseDTO create(EmployerCreateDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (employerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        Employer employer = employerMapper.toEntity(dto);
        employer.setEmail(email);
        // CORRECTION : encodage après mapping
        employer.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        employer.setActif(true);
        if (employer.getDateEmbauche() == null) {
            employer.setDateEmbauche(LocalDate.now());
        }

        resolvePosteAndRole(employer, dto.getPosteId(), dto.getRoleId());

        employer = employerRepository.save(employer);
        log.info("Employé créé : {} ({})", employer.getName(), employer.getEmail());
        return employerMapper.toResponseDTO(employer);
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<EmployerResponseDTO> findAll(Pageable pageable) {
        return employerRepository.findAll(pageable)
                .map(employerMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public EmployerResponseDTO findById(Long id) {
        Employer employer = employerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));
        return employerMapper.toResponseDTO(employer);
    }

    @Transactional
    public EmployerResponseDTO update(Long id, EmployerUpdateDTO dto) {
        Employer employer = employerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));

        // CORRECTION : updateEntity() avec EmployerUpdateDTO (sans motDePasse, sans email)
        employerMapper.updateEntity(employer, dto);

        // Poste et rôle
        if (dto.getPosteId() != null) {
            resolvePosteAndRole(employer, dto.getPosteId(), dto.getRoleId());
        } else if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + dto.getRoleId()));
            employer.setRole(role);
        }

        employer = employerRepository.save(employer);
        return employerMapper.toResponseDTO(employer);
    }

    /**
     * CORRECTION : changement de mot de passe via endpoint dédié.
     * Séparé de update() — requiert l'ancien mot de passe pour validation.
     */
    @Transactional
    public void changePassword(Long id, String ancienMotDePasse, String nouveauMotDePasse) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));

        if (!passwordEncoder.matches(ancienMotDePasse, employer.getMotDePasse())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }
        if (nouveauMotDePasse.length() < 8) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 8 caractères");
        }
        employer.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        employerRepository.save(employer);
        log.info("Mot de passe modifié pour l'employé : {}", employer.getEmail());
    }

    @Transactional
    public void delete(Long id) {
        if (!employerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé non trouvé : " + id);
        }
        employerRepository.deleteById(id);
        log.info("Employé supprimé : {}", id);
    }

    @Transactional
    public EmployerResponseDTO toggleActif(Long id) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));
        employer.setActif(!employer.isActif());
        employer = employerRepository.save(employer);
        log.info("Employé {} : actif = {}", employer.getEmail(), employer.isActif());
        return employerMapper.toResponseDTO(employer);
    }

    /**
     * CORRECTION : stats séparées de findById() pour éviter de les charger à chaque requête.
     * Exposé via GET /employers/{id}/stats
     */
    public EmployerStatsDTO getStats(Long id) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));

        EmployerStatsDTO stats = new EmployerStatsDTO();
        stats.setEmployerId(id);
        stats.setEmployerNom(employer.getName());

        // Tâches — via COUNT, pas en chargeant les collections
        stats.setNombreTaches(tacheRepository.countByAssigneeId(id));
        stats.setNombreTachesTerminees(tacheRepository.countByAssigneeIdAndStatut(id, Tache.StatutTache.TERMINEE));
        stats.setNombreTachesEnCours(tacheRepository.countByAssigneeIdAndStatut(id, Tache.StatutTache.EN_COURS));
        stats.setNombreTachesEnRetard(tacheRepository.countTachesEnRetardByAssigneeId(id, LocalDate.now(), Tache.StatutTache.TERMINEE));

        // Événements
        stats.setNombreEvenements(evenementRepository.countByCreateurId(id));

        // Documents
        stats.setNombreDocuments(documentRepository.countByEmployerId(id));

        // Factures
        stats.setNombreFacturesEmises(factureRepository.countByEmetteurId(id));
        stats.setNombreFacturesRecues(factureRepository.countByClientId(id));
        stats.setMontantTotalFacturesEmises(factureRepository.sumMontantTTCByEmetteurId(id));
        stats.setMontantTotalFacturesRecues(factureRepository.sumMontantTTCByClientId(id));

        return stats;
    }

    // ─── Utilitaire ──────────────────────────────────────────

    private void resolvePosteAndRole(Employer employer, Long posteId, Long roleId) {
        if (posteId != null) {
            Poste poste = posteRepository.findById(posteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
            employer.setPoste(poste);
            // Si pas de rôle explicite, hériter du poste
            if (roleId == null && poste.getRole() != null) {
                employer.setRole(poste.getRole());
                return;
            }
        }
        if (roleId != null) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + roleId));
            employer.setRole(role);
        }
    }
}