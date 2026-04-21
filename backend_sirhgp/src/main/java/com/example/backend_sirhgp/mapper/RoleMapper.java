package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.RoleResponseDTO;
import com.example.backend_sirhgp.model.Permission;
import com.example.backend_sirhgp.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {

    @Mapping(target = "permissions",    expression = "java(mapPermissionsToSummary(role.getPermissions()))")
    @Mapping(target = "nombreEmployes", ignore = true)
    @Mapping(target = "nombrePostes",   ignore = true)
    RoleResponseDTO toResponseDTO(Role role);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "permissions",      ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Role entity, String name, String description);

    default Set<RoleResponseDTO.PermissionSummaryDTO> mapPermissionsToSummary(Set<Permission> permissions) {
        if (permissions == null) return null;
        return permissions.stream().map(p -> {
            RoleResponseDTO.PermissionSummaryDTO dto = new RoleResponseDTO.PermissionSummaryDTO();
            dto.setId(p.getId());
            dto.setName(p.getName());
            return dto;
        }).collect(Collectors.toSet());
    }
}