package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.RoleRequestDTO;
import com.example.backend_sirhgp.dto.response.RoleResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.RoleMapper;
import com.example.backend_sirhgp.model.Permission;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.repository.PermissionRepository;
import com.example.backend_sirhgp.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Rôle déjà existant : " + dto.getName());
        }
        Role role = Role.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .build();
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            role.getPermissions().addAll(loadPermissions(dto.getPermissionIds()));
        }
        role = roleRepository.save(role);
        log.info("Rôle créé : {}", role.getName());
        return enrich(roleMapper.toResponseDTO(role), role.getId());
    }

    @Transactional(readOnly = true)
    public Page<RoleResponseDTO> findAll(Pageable pageable) {
        return roleRepository.findAllWithPermissions(pageable)
                .map(r -> enrich(roleMapper.toResponseDTO(r), r.getId()));
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO findById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + id));
        return enrich(roleMapper.toResponseDTO(role), id);
    }

    @Transactional
    public RoleResponseDTO update(Long id, RoleRequestDTO dto) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + id));
        if (dto.getName() != null) role.setName(dto.getName().trim());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getPermissionIds() != null) {
            role.clearPermissions();
            role.getPermissions().addAll(loadPermissions(dto.getPermissionIds()));
        }
        return enrich(roleMapper.toResponseDTO(roleRepository.save(role)), id);
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rôle non trouvé : " + id);
        }
        roleRepository.deleteById(id);
        log.info("Rôle supprimé : {}", id);
    }

    @Transactional
    public RoleResponseDTO addPermission(Long roleId, Long permId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + roleId));
        Permission perm = permissionRepository.findById(permId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permId));
        role.addPermission(perm);
        return enrich(roleMapper.toResponseDTO(roleRepository.save(role)), roleId);
    }

    @Transactional
    public RoleResponseDTO removePermission(Long roleId, Long permId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + roleId));
        Permission perm = permissionRepository.findById(permId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + permId));
        role.removePermission(perm);
        return enrich(roleMapper.toResponseDTO(roleRepository.save(role)), roleId);
    }

    @Transactional
    public RoleResponseDTO replacePermissions(Long roleId, Set<Long> permIds) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé : " + roleId));
        role.clearPermissions();
        if (permIds != null) {
            role.getPermissions().addAll(loadPermissions(permIds));
        }
        return enrich(roleMapper.toResponseDTO(roleRepository.save(role)), roleId);
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private Set<Permission> loadPermissions(Set<Long> ids) {
        return ids.stream()
                .map(pid -> permissionRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission non trouvée : " + pid)))
                .collect(Collectors.toSet());
    }

    private RoleResponseDTO enrich(RoleResponseDTO dto, Long roleId) {
        dto.setNombreEmployes(roleRepository.countEmployesActifsByRoleId(roleId));
        dto.setNombrePostes(roleRepository.countPostesByRoleId(roleId));
        return dto;
    }
}