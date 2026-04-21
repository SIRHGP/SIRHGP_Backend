package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.PermissionRequestDTO;
import com.example.backend_sirhgp.dto.response.PermissionResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.PermissionMapper;
import com.example.backend_sirhgp.model.Action;
import com.example.backend_sirhgp.model.Permission;
import com.example.backend_sirhgp.repository.ActionRepository;
import com.example.backend_sirhgp.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final ActionRepository actionRepository;
    private final PermissionMapper permissionMapper;

    @Transactional
    public PermissionResponseDTO create(PermissionRequestDTO dto) {
        if (permissionRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Permission déjà existante : " + dto.getName());
        }
        Permission permission = Permission.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .build();
        if (dto.getActionIds() != null && !dto.getActionIds().isEmpty()) {
            permission.addActions(loadActions(dto.getActionIds()));
        }
        permission = permissionRepository.save(permission);
        log.info("Permission créée : {}", permission.getName());
        return enrich(permissionMapper.toResponseDTO(permission), permission.getId());
    }

//    @Transactional(readOnly = true)
//    public Page<PermissionResponseDTO> findAll(int page, int size, String sortBy, String direction) {
//        Pageable pageable = PageRequest.of(page, size,
//                Sort.by(Sort.Direction.fromString(direction), sortBy));
//        return permissionRepository.findAllWithActions(pageable)
//                .map(p -> enrich(permissionMapper.toResponseDTO(p), p.getId()));
//    }

    @Transactional(readOnly = true)
    public Page<PermissionResponseDTO> findAll(Pageable pageable) {
        return permissionRepository.findAllWithActions(pageable)
                .map(p -> enrich(permissionMapper.toResponseDTO(p), p.getId()));
    }

    @Transactional(readOnly = true)
    public PermissionResponseDTO findById(Long id) {
        Permission permission = permissionRepository.findByIdWithActions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + id));
        return enrich(permissionMapper.toResponseDTO(permission), id);
    }

    @Transactional
    public PermissionResponseDTO update(Long id, PermissionRequestDTO dto) {
        Permission permission = permissionRepository.findByIdWithActions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + id));
        if (dto.getName() != null) permission.setName(dto.getName().trim());
        if (dto.getDescription() != null) permission.setDescription(dto.getDescription());
        if (dto.getActionIds() != null) {
            permission.clearActions();
            permission.addActions(loadActions(dto.getActionIds()));
        }
        permission = permissionRepository.save(permission);
        return enrich(permissionMapper.toResponseDTO(permission), id);
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission non trouvée : " + id);
        }
        permissionRepository.deleteById(id);
        log.info("Permission supprimée : {}", id);
    }

    @Transactional
    public PermissionResponseDTO addAction(Long permissionId, Long actionId) {
        Permission permission = permissionRepository.findByIdWithActions(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permissionId));
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée : " + actionId));
        permission.addAction(action);
        return enrich(permissionMapper.toResponseDTO(permissionRepository.save(permission)), permissionId);
    }

    @Transactional
    public PermissionResponseDTO removeAction(Long permissionId, Long actionId) {
        Permission permission = permissionRepository.findByIdWithActions(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permissionId));
        Action action = actionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée : " + actionId));
        permission.removeAction(action);
        return enrich(permissionMapper.toResponseDTO(permissionRepository.save(permission)), permissionId);
    }

    @Transactional
    public PermissionResponseDTO replaceActions(Long permissionId, Set<Long> actionIds) {
        Permission permission = permissionRepository.findByIdWithActions(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permissionId));
        permission.clearActions();
        if (actionIds != null && !actionIds.isEmpty()) {
            permission.addActions(loadActions(actionIds));
        }
        return enrich(permissionMapper.toResponseDTO(permissionRepository.save(permission)), permissionId);
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private Set<Action> loadActions(Set<Long> ids) {
        return ids.stream()
                .map(aid -> actionRepository.findById(aid)
                        .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée : " + aid)))
                .collect(Collectors.toSet());
    }

    private PermissionResponseDTO enrich(PermissionResponseDTO dto, Long permissionId) {
        dto.setNombreRoles(permissionRepository.countRolesByPermissionId(permissionId));
        dto.setNombrePostes(permissionRepository.countPostesByPermissionId(permissionId));
        return dto;
    }
}