package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.PosteRequestDTO;
import com.example.backend_sirhgp.dto.response.PosteResponseDTO;
import com.example.backend_sirhgp.model.Permission;
import com.example.backend_sirhgp.model.Poste;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORRECTION : mapPermissionsToSummary() retourne Set<PosteResponseDTO.PermissionSummaryDTO>
 * (inner class locale) et non PermissionResponseDTO.PermissionSummaryDTO qui n'est pas
 * accessible depuis un autre fichier sans qualification complète.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PosteMapper {

    @Mapping(source = "role.id",          target = "roleId")
    @Mapping(source = "role.name",        target = "roleName")
    @Mapping(source = "departement.id",   target = "departementId")
    @Mapping(source = "departement.name", target = "departementName")
    @Mapping(target = "permissions",      expression = "java(mapPermissionsToSummary(poste.getPermissions()))")
    @Mapping(target = "nombreEmployes",   ignore = true)
    PosteResponseDTO toResponseDTO(Poste poste);

    @Mapping(target = "id",              ignore = true)  // CORRECTION
    @Mapping(target = "role",             ignore = true)
    @Mapping(target = "departement",      ignore = true)
    @Mapping(target = "permissions",      ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Poste entity, String name, String description);

    default Set<PosteResponseDTO.PermissionSummaryDTO> mapPermissionsToSummary(Set<Permission> permissions) {
        if (permissions == null) return null;
        return permissions.stream().map(p -> {
            PosteResponseDTO.PermissionSummaryDTO dto = new PosteResponseDTO.PermissionSummaryDTO();
            dto.setId(p.getId());
            dto.setName(p.getName());
            return dto;
        }).collect(Collectors.toSet());
    }
}