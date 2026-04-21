package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.PosteRequestDTO;
import com.example.backend_sirhgp.dto.response.PosteResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.PosteMapper;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Permission;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.PermissionRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import com.example.backend_sirhgp.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORRECTIONS :
 * - PosteRequestDTO / PosteResponseDTO au lieu de l'ancien PosteDTO.
 * - findAll() paginé.
 * - nombreEmployes enrichi via COUNT dans le repository.
 * - posteMapper.toDTO() → posteMapper.toResponseDTO().
 * - posteMapper.toEntity(dto) → construction manuelle depuis PosteRequestDTO
 *   (les relations rôle/département/permissions sont toujours résolues manuellement).
 * - updateEntity() adapté au nouveau mapper (paramètres name + description).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosteService {

    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final DepartementRepository departementRepository;
    private final PermissionRepository permissionRepository;
    private final PosteMapper posteMapper;

    @Transactional
    public PosteResponseDTO create(PosteRequestDTO dto) {
        if (posteRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Poste déjà existant : " + dto.getName());
        }

        // Construction manuelle : les relations sont résolues explicitement
        Poste poste = Poste.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .build();

        resolveRelations(poste, dto.getRoleId(), dto.getDepartementId(), dto.getPermissionIds());

        poste = posteRepository.save(poste);
        log.info("Poste créé : {}", poste.getName());
        return enrich(posteMapper.toResponseDTO(poste), poste.getId());
    }

    // CORRECTION : pagination
    @Transactional(readOnly = true)
    public Page<PosteResponseDTO> findAll(Pageable pageable) {
        return posteRepository.findAll(pageable)
                .map(p -> enrich(posteMapper.toResponseDTO(p), p.getId()));
    }

    @Transactional(readOnly = true)
    public PosteResponseDTO findById(Long id) {
        Poste poste = posteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + id));
        return enrich(posteMapper.toResponseDTO(poste), id);
    }

    @Transactional(readOnly = true)
    public List<PosteResponseDTO> findByDepartement(Long departementId) {
        return posteRepository.findByDepartementId(departementId).stream()
                .map(p -> enrich(posteMapper.toResponseDTO(p), p.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public PosteResponseDTO update(Long id, PosteRequestDTO dto) {
        Poste poste = posteRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + id));

        // CORRECTION : updateEntity(entity, name, description) — signature du mapper corrigé
        if (dto.getName() != null && !dto.getName().isBlank()) {
            if (!dto.getName().equals(poste.getName()) && posteRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Ce nom de poste est déjà utilisé");
            }
            poste.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            poste.setDescription(dto.getDescription());
        }

        // Rôle
        if (dto.getRoleId() != null) {
            Role role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + dto.getRoleId()));
            poste.setRole(role);
        } else {
            poste.setRole(null);
        }

        // Département
        if (dto.getDepartementId() != null) {
            Departement dept = departementRepository.findById(dto.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + dto.getDepartementId()));
            poste.setDepartement(dept);
        } else {
            poste.setDepartement(null);
        }

        // Permissions
        if (dto.getPermissionIds() != null) {
            poste.clearPermissions();
            poste.addPermissions(loadPermissions(dto.getPermissionIds()));
        }

        poste = posteRepository.save(poste);
        return enrich(posteMapper.toResponseDTO(poste), id);
    }

    @Transactional
    public void delete(Long id) {
        if (!posteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Poste non trouvé : " + id);
        }
        posteRepository.deleteById(id);
        log.info("Poste supprimé : {}", id);
    }

    // ─── Gestion des permissions ──────────────────────────────

    @Transactional
    public PosteResponseDTO addPermission(Long posteId, Long permissionId) {
        Poste poste = posteRepository.findByIdWithPermissions(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
        Permission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permissionId));
        poste.addPermission(perm);
        return enrich(posteMapper.toResponseDTO(posteRepository.save(poste)), posteId);
    }

    @Transactional
    public PosteResponseDTO removePermission(Long posteId, Long permissionId) {
        Poste poste = posteRepository.findByIdWithPermissions(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
        Permission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permissionId));
        poste.removePermission(perm);
        return enrich(posteMapper.toResponseDTO(posteRepository.save(poste)), posteId);
    }

    @Transactional
    public PosteResponseDTO replacePermissions(Long posteId, Set<Long> permissionIds) {
        Poste poste = posteRepository.findByIdWithPermissions(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
        poste.clearPermissions();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            poste.addPermissions(loadPermissions(permissionIds));
        }
        return enrich(posteMapper.toResponseDTO(posteRepository.save(poste)), posteId);
    }

    // ─── Gestion du département ───────────────────────────────

    @Transactional
    public PosteResponseDTO assignToDepartement(Long posteId, Long departementId) {
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
        Departement dept = departementRepository.findById(departementId)
                .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + departementId));
        poste.setDepartement(dept);
        return enrich(posteMapper.toResponseDTO(posteRepository.save(poste)), posteId);
    }

    @Transactional
    public PosteResponseDTO removeFromDepartement(Long posteId) {
        Poste poste = posteRepository.findById(posteId)
                .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + posteId));
        poste.setDepartement(null);
        return enrich(posteMapper.toResponseDTO(posteRepository.save(poste)), posteId);
    }

    // ─── Utilitaires ─────────────────────────────────────────

    private void resolveRelations(Poste poste, Long roleId, Long departementId, Set<Long> permissionIds) {
        if (roleId != null) {
            poste.setRole(roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + roleId)));
        }
        if (departementId != null) {
            poste.setDepartement(departementRepository.findById(departementId)
                    .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + departementId)));
        }
        if (permissionIds != null && !permissionIds.isEmpty()) {
            poste.addPermissions(loadPermissions(permissionIds));
        }
    }

    private Set<Permission> loadPermissions(Set<Long> ids) {
        return ids.stream()
                .map(pid -> permissionRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + pid)))
                .collect(Collectors.toSet());
    }

    /**
     * Enrichit le DTO avec le nombre d'employés actifs du poste.
     * Utilise un COUNT en DB — jamais en chargeant la collection.
     */
    private PosteResponseDTO enrich(PosteResponseDTO dto, Long posteId) {
        dto.setNombreEmployes(posteRepository.countEmployesActifsByPosteId(posteId));
        return dto;
    }
}