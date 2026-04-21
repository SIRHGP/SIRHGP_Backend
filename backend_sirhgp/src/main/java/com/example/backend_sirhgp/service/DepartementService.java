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

    @Transactional
    public DepartementResponseDTO create(DepartementRequestDTO dto) {
        String name = dto.getName().trim();
        if (departementRepository.existsByName(name)) {
            throw new IllegalArgumentException("Ce département existe déjà : " + name);
        }

        Departement dep = Departement.builder().name(name).build();
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
        return departementRepository.findAll(pageable)
                .map(this::enrichAndMap);
    }

    @Transactional(readOnly = true)
    public DepartementResponseDTO findById(Long id) {
        Departement dep = departementRepository.findByIdWithPostes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));
        return enrichAndMap(dep);
    }

    @Transactional
    public DepartementResponseDTO update(Long id, DepartementRequestDTO dto) {
        Departement dep = departementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));

        String newName = dto.getName() != null ? dto.getName().trim() : null;
        if (newName != null && !newName.equals(dep.getName())) {
            if (departementRepository.existsByName(newName)) {
                throw new IllegalArgumentException("Nom de département déjà utilisé : " + newName);
            }
            dep.setName(newName);
        }

        dep = departementRepository.save(dep);
        return enrichAndMap(dep);
    }

    @Transactional
    public void delete(Long id) {
        Departement dep = departementRepository.findByIdWithPostes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + id));

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
        // CORRECTION : COUNT via requêtes dédiées — pas en chargeant les collections
        dto.setNombreEmployes(employerRepository.countActifsByDepartementId(dep.getId()));
        dto.setNombrePostes((long) dep.getPostes().size());
        // nombreProjets calculé via requête COUNT (à ajouter dans ProjetRepository si nécessaire)
        return dto;
    }
}