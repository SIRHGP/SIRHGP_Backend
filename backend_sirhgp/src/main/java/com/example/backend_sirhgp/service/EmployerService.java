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
import com.example.backend_sirhgp.model.Sucursal;
import com.example.backend_sirhgp.model.Entreprise;
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
    private final SucursalRepository sucursalRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseContextService entrepriseContextService;

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
        appliquerEntrepriseEtSuccursale(employer, dto.getEntrepriseId(), dto.getSucursalId());
        appliquerManager(employer, dto.getManagerId());
        verifierUniciteSecretaire(employer);

        employer = employerRepository.save(employer);
        log.info("Employé créé : {} ({})", employer.getName(), employer.getEmail());
        return toResponseDTO(employer);
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<EmployerResponseDTO> findAll(Pageable pageable) {
        String role = entrepriseContextService.currentRole();
        if ("SUPER_ADMIN".equals(role)) {
            return employerRepository.findAll(pageable).map(this::toResponseDTO);
        }

        // L'ADMIN pilote toutes les succursales de son entreprise, jamais les
        // comptes d'une autre entreprise. La liste globale précédente exposait
        // des employés qu'il ne pouvait ensuite ni modifier ni désactiver.
        Employer courant = entrepriseContextService.getCurrentEmployer();
        if ("ADMIN".equals(role)) {
            if (courant == null || courant.getEntreprise() == null) {
                return Page.empty(pageable);
            }
            java.util.List<EmployerResponseDTO> entreprise = employerRepository
                    .findByEntrepriseId(courant.getEntreprise().getId()).stream()
                    .map(this::toResponseDTO).toList();
            return page(entreprise, pageable);
        }

        java.util.List<EmployerResponseDTO> scoped = employerRepository.findAll().stream()
                .filter(this::employerDansPerimetre)
                .map(this::toResponseDTO).toList();
        return page(scoped, pageable);
    }

    private Page<EmployerResponseDTO> page(java.util.List<EmployerResponseDTO> content, Pageable pageable) {
        if (pageable.isUnpaged()) return new org.springframework.data.domain.PageImpl<>(content);
        int from = Math.min((int) pageable.getOffset(), content.size());
        int to = Math.min(from + pageable.getPageSize(), content.size());
        return new org.springframework.data.domain.PageImpl<>(content.subList(from, to), pageable, content.size());
    }

    @Transactional(readOnly = true)
    public java.util.List<EmployerResponseDTO> findLeads() {
        return employerRepository.findAll().stream()
                .filter(this::employerDansPerimetre)
                .filter(e -> e.isActif() && e.getRole() != null && "LEAD".equals(e.getRole().getName()))
                .map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<EmployerResponseDTO> findManagers() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null || current.getEntreprise() == null) return java.util.List.of();
        return employerRepository.findManagersActifsByEntrepriseId(current.getEntreprise().getId())
                .stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<EmployerResponseDTO> findOrganigramme() {
        return employerRepository.findAll().stream()
                .filter(this::employerDansPerimetre)
                .map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public EmployerResponseDTO findById(Long id) {
        Employer employer = employerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));
        verifierPerimetre(employer);
        return toResponseDTO(employer);
    }

    @Transactional
    public EmployerResponseDTO update(Long id, EmployerUpdateDTO dto) {
        Employer employer = employerRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));

        // CORRECTION : updateEntity() avec EmployerUpdateDTO (sans motDePasse, sans email)
        employerMapper.updateEntity(employer, dto);

        appliquerEntrepriseEtSuccursale(employer, dto.getEntrepriseId(), dto.getSucursalId());
        appliquerManager(employer, dto.getManagerId());

        // Poste et rôle
        if (dto.getPosteId() != null) {
            resolvePosteAndRole(employer, dto.getPosteId(), dto.getRoleId());
        } else if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + dto.getRoleId()));
            employer.setRole(role);
        }

        verifierUniciteSecretaire(employer);

        employer = employerRepository.save(employer);
        return toResponseDTO(employer);
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
        if (!employerDansPerimetre(employer)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Employé hors de votre entreprise.");
        }
        Employer courant = entrepriseContextService.getCurrentEmployer();
        if (employer.isActif() && courant != null && courant.getId().equals(id)) {
            throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        employer.setActif(!employer.isActif());
        verifierUniciteSecretaire(employer);
        employer = employerRepository.save(employer);
        log.info("Employé {} : actif = {}", employer.getEmail(), employer.isActif());
        return toResponseDTO(employer);
    }

    /**
     * CORRECTION : stats séparées de findById() pour éviter de les charger à chaque requête.
     * Exposé via GET /employers/{id}/stats
     */
    public EmployerStatsDTO getStats(Long id) {
        Employer employer = employerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + id));
        verifierPerimetre(employer);

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

    private boolean employerDansPerimetre(Employer cible) {
        String role = entrepriseContextService.currentRole();
        if ("SUPER_ADMIN".equals(role)) return true;
        Employer courant = entrepriseContextService.getCurrentEmployer();
        if (courant == null || cible.getEntreprise() == null || courant.getEntreprise() == null
                || !courant.getEntreprise().getId().equals(cible.getEntreprise().getId())) return false;
        if ("MANAGER".equals(role) || "LEAD".equals(role)) {
            if (courant.getSucursal() == null || cible.getSucursal() == null
                    || !courant.getSucursal().getId().equals(cible.getSucursal().getId())) return false;
        }
        if ("LEAD".equals(role) && cible.getPoste() != null && cible.getPoste().getDepartement() != null) {
            return entrepriseContextService.employeDansLePerimetre(cible.getId());
        }
        return true;
    }

    private void verifierPerimetre(Employer cible) {
        String role = entrepriseContextService.currentRole();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !employerDansPerimetre(cible)) {
            throw new org.springframework.security.access.AccessDeniedException("Employé hors de votre périmètre.");
        }
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

    private void appliquerEntrepriseEtSuccursale(Employer employer, Long entrepriseId, Long sucursalId) {
        if (sucursalId != null) {
            Sucursal sucursal = sucursalRepository.findByIdForUpdate(sucursalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée : " + sucursalId));
            if (entrepriseId != null && (sucursal.getEntreprise() == null || !entrepriseId.equals(sucursal.getEntreprise().getId())))
                throw new IllegalArgumentException("La succursale n'appartient pas à l'entreprise sélectionnée.");
            employer.setSucursal(sucursal);
            employer.setEntreprise(sucursal.getEntreprise());
        } else if (entrepriseId != null) {
            Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise non trouvée : " + entrepriseId));
            employer.setEntreprise(entreprise);
            employer.setSucursal(null);
        }
    }

    private void appliquerManager(Employer employer, Long managerId) {
        if (managerId == null) return;
        if (employer.getId() != null && employer.getId().equals(managerId)) {
            throw new IllegalArgumentException("Un employé ne peut pas être son propre responsable.");
        }
        Employer manager = employerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsable non trouvé : " + managerId));
        if (!manager.isActif()) {
            throw new IllegalArgumentException("Le responsable sélectionné est inactif.");
        }
        if (employer.getEntreprise() == null || manager.getEntreprise() == null
                || !employer.getEntreprise().getId().equals(manager.getEntreprise().getId())) {
            throw new IllegalArgumentException("Le responsable doit appartenir à la même entreprise.");
        }
        employer.setManager(manager);
    }

    private EmployerResponseDTO toResponseDTO(Employer employer) {
        EmployerResponseDTO dto = employerMapper.toResponseDTO(employer);
        String role = entrepriseContextService.currentRole();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            // Les données de paie et bancaires sont réservées aux rôles de gestion.
            dto.setSalaireBase(null);
            dto.setNui(null);
            dto.setRib(null);
        }
        return dto;
    }

    private void verifierUniciteSecretaire(Employer employer) {
        if (employer.getRole() == null || !"SECRETAIRE".equals(employer.getRole().getName()) || !employer.isActif()) return;
        if (employer.getSucursal() == null || employer.getEntreprise() == null)
            throw new IllegalArgumentException("Une secrétaire active doit être associée à une succursale.");
        sucursalRepository.findByIdForUpdate(employer.getSucursal().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée"));
        if (employerRepository.countOtherActiveSecretaries(employer.getSucursal().getId(), employer.getId()) > 0)
            throw new IllegalArgumentException("Cette succursale possède déjà une secrétaire active.");
    }
}
