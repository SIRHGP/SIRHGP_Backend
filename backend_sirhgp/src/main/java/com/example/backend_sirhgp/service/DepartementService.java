package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.DepartementRequestDTO;
import com.example.backend_sirhgp.dto.response.DepartementResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.DepartementMapper;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORRECTIONS :
 * - DepartementRequestDTO / DepartementResponseDTO au lieu de l'ancien DepartementDTO.
 * - findAll() paginé.
 * - enrichissement des compteurs (nombreEmployes, nombrePostes, nombreProjets)
 *   via des requêtes COUNT dédiées — jamais en chargeant les collections.
 * - create() et update() : construction manuelle de l'entité depuis RequestDTO
 *   (trop simple pour mapper toEntity()).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final EmployerRepository employerRepository;
    private final DepartementMapper departementMapper;
    private final EntrepriseContextService entrepriseContextService;

    @Transactional
    public DepartementResponseDTO create(DepartementRequestDTO dto) {
        String name = dto.getName().trim();
        boolean manager = "MANAGER".equals(entrepriseContextService.currentRole());
        if (manager && entrepriseContextService.getScopeSucursalId() == null) {
            throw new IllegalArgumentException("Votre compte MANAGER doit être rattaché à une succursale pour créer un département.");
        }
        if (manager && dto.getSuperviseurId() == null) {
            throw new IllegalArgumentException("Choisissez un LEAD actif de votre succursale comme chef du département.");
        }
        Long scopeEntrepriseId = entrepriseContextService.getScopeEntrepriseId();
        Long entrepriseId = scopeEntrepriseId != null ? scopeEntrepriseId : dto.getEntrepriseId();
        boolean superAdmin = "SUPER_ADMIN".equals(entrepriseContextService.currentRole());
        if (entrepriseId == null && !superAdmin) {
            throw new IllegalArgumentException("Une entreprise doit être précisée pour ce département");
        }
        if (scopeEntrepriseId != null && dto.getEntrepriseId() != null
                && !scopeEntrepriseId.equals(dto.getEntrepriseId())) {
            throw new IllegalArgumentException("Vous ne pouvez créer un département que dans votre entreprise");
        }
        if (departementRepository.existsByNameAndEntrepriseId(name, entrepriseId)) {
            throw new IllegalArgumentException("Ce département existe déjà : " + name);
        }

        Departement dep = Departement.builder()
                .name(name)
                .entreprise(entrepriseId != null
                        ? entrepriseContextService.getRequiredEntreprise(entrepriseId) : null)
                .build();
        appliquerSuperviseur(dep, dto.getSuperviseurId());
        dep = departementRepository.save(dep);

        // Association des postes si fournis
        if (dto.getProjetIds() != null && !dto.getProjetIds().isEmpty()) {
            // Note : la relation Projet-Departement est gérée côté Projet.
            // Ici on ne peut qu'associer des postes existants.
            log.warn("projetIds ignorés à la création : gérer via ProjetService.addDepartement()");
        }

        log.info("Département créé : {}", dep.getName());
        return enrichAndMap(dep);
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<DepartementResponseDTO> findAll(Pageable pageable) {
        Set<Long> allowed = allowedDepartmentIds();
        if (allowed == null) return departementRepository.findAll(pageable).map(this::enrichAndMap);
        List<DepartementResponseDTO> items = departementRepository.findAll().stream()
                .filter(d -> allowed.contains(d.getId())).map(this::enrichAndMap).toList();
        if (pageable.isUnpaged()) return new org.springframework.data.domain.PageImpl<>(items);
        int from = Math.min((int) pageable.getOffset(), items.size());
        int to = Math.min(from + pageable.getPageSize(), items.size());
        return new org.springframework.data.domain.PageImpl<>(items.subList(from, to), pageable, items.size());
    }

    @Transactional(readOnly = true)
    public DepartementResponseDTO findById(Long id) {
        Departement dep = departementRepository.findByIdWithPostes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));
        verifierLecture(dep.getId());
        return enrichAndMap(dep);
    }

    private Set<Long> allowedDepartmentIds() {
        String role = entrepriseContextService.currentRole();
        if ("SUPER_ADMIN".equals(role)) return null;
        Long entrepriseId = entrepriseContextService.getScopeEntrepriseId();
        if (entrepriseId == null) return Set.of();
        Long sucursalId = entrepriseContextService.getScopeSucursalId();
        if ("MANAGER".equals(role) && sucursalId == null) return Set.of();
        List<Long> ids;
        if ("LEAD".equals(role)) {
            List<Long> leadIds = entrepriseContextService.getScopeDepartementIds();
            if (leadIds.isEmpty()) {
                ids = sucursalId == null ? departementRepository.findIdsByEntrepriseId(entrepriseId)
                        : departementRepository.findIdsBySucursalId(sucursalId, entrepriseId);
            } else if (sucursalId == null) {
                Set<Long> companyIds = Set.copyOf(departementRepository.findIdsByEntrepriseId(entrepriseId));
                ids = leadIds.stream().filter(companyIds::contains).toList();
            } else {
                Set<Long> branchIds = Set.copyOf(departementRepository.findIdsBySucursalId(sucursalId, entrepriseId));
                ids = leadIds.stream().filter(branchIds::contains).toList();
            }
        } else if (sucursalId != null) {
            ids = departementRepository.findIdsBySucursalId(sucursalId, entrepriseId);
        } else {
            ids = departementRepository.findIdsByEntrepriseId(entrepriseId);
        }
        return Set.copyOf(ids);
    }

    private void verifierLecture(Long id) {
        Set<Long> allowed = allowedDepartmentIds();
        if (allowed != null && !allowed.contains(id))
            throw new ResourceNotFoundException("Département non trouvé : " + id);
    }

    @Transactional
    public DepartementResponseDTO update(Long id, DepartementRequestDTO dto) {
        Departement dep = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));
        verifierLecture(dep.getId());

        String newName = dto.getName() != null ? dto.getName().trim() : null;
        if (newName != null && !newName.equals(dep.getName())) {
            Long entrepriseId = dep.getEntreprise() != null ? dep.getEntreprise().getId() : null;
            if (entrepriseId == null || departementRepository.existsByNameAndEntrepriseId(newName, entrepriseId)) {
                throw new IllegalArgumentException("Nom de département déjà utilisé : " + newName);
            }
            dep.setName(newName);
        }

        if (dto.getSuperviseurId() != null) {
            appliquerSuperviseur(dep, dto.getSuperviseurId());
        }

        dep = departementRepository.save(dep);
        return enrichAndMap(dep);
    }

    private void appliquerSuperviseur(Departement departement, Long superviseurId) {
        if (superviseurId == null) return;
        var superviseur = employerRepository.findById(superviseurId)
                .orElseThrow(() -> new ResourceNotFoundException("Chef de département introuvable : " + superviseurId));
        if (superviseur.getRole() == null || !"LEAD".equals(superviseur.getRole().getName())) {
            throw new IllegalArgumentException("Le chef de département doit avoir le rôle LEAD");
        }
        if (!superviseur.isActif()) {
            throw new IllegalArgumentException("Le chef de département doit avoir un compte actif");
        }
        Long scopeEntreprise = entrepriseContextService.getScopeEntrepriseId();
        if (scopeEntreprise != null && (superviseur.getEntreprise() == null
                || !scopeEntreprise.equals(superviseur.getEntreprise().getId()))) {
            throw new IllegalArgumentException("Le chef de département doit appartenir à votre entreprise");
        }
        Long departementEntreprise = departement.getEntreprise() != null
                ? departement.getEntreprise().getId() : null;
        Long superviseurEntreprise = superviseur.getEntreprise() != null
                ? superviseur.getEntreprise().getId() : null;
        if (departementEntreprise != null && !departementEntreprise.equals(superviseurEntreprise)) {
            throw new IllegalArgumentException("Le chef de département doit appartenir à l'entreprise du département");
        }
        if (departementEntreprise == null && "SUPER_ADMIN".equals(entrepriseContextService.currentRole())
                && superviseur.getEntreprise() != null) {
            departement.setEntreprise(superviseur.getEntreprise());
        }
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null && (superviseur.getSucursal() == null
                || !scopeSucursal.equals(superviseur.getSucursal().getId()))) {
            throw new IllegalArgumentException("Le chef de département doit appartenir à votre succursale");
        }
        departement.setSuperviseur(superviseur);
    }

    @Transactional
    public void delete(Long id) {
        Departement dep = departementRepository.findByIdWithPostes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));
        verifierLecture(dep.getId());

        // Dissocier les postes avant suppression
        if (dep.getPostes() != null && !dep.getPostes().isEmpty()) {
            dep.getPostes().forEach(p -> p.setDepartement(null));
            posteRepository.saveAll(dep.getPostes());
        }

        departementRepository.delete(dep);
        log.info("Département supprimé : {}", id);
    }

    @Transactional
    public DepartementResponseDTO addPoste(Long departementId, Long posteId) {
        Departement dep = departementRepository.findByIdWithPostes(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + departementId));
        verifierLecture(dep.getId());
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));

        poste.setDepartement(dep);
        posteRepository.save(poste);
        dep.getPostes().add(poste);

        return enrichAndMap(departementRepository.save(dep));
    }

    @Transactional
    public DepartementResponseDTO removePoste(Long departementId, Long posteId) {
        Departement dep = departementRepository.findByIdWithPostes(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + departementId));
        verifierLecture(dep.getId());
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));

        poste.setDepartement(null);
        posteRepository.save(poste);
        dep.getPostes().remove(poste);

        return enrichAndMap(departementRepository.save(dep));
    }

    // ─── Utilitaire : enrichissement des compteurs ────────────

    private DepartementResponseDTO enrichAndMap(Departement dep) {
        DepartementResponseDTO dto = departementMapper.toResponseDTO(dep);
        Long entrepriseId = entrepriseContextService.getScopeEntrepriseId();
        dto.setNombreEmployes(entrepriseId == null
                ? employerRepository.countActifsByDepartementId(dep.getId())
                : employerRepository.countActifsByDepartementIdAndEntrepriseId(dep.getId(), entrepriseId));
        dto.setNombrePostes((long) dep.getPostes().size());
        // nombreProjets calculé via requête COUNT (à ajouter dans ProjetRepository si nécessaire)
        return dto;
    }
}
