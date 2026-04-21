package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.PermissionResponseDTO;
import com.example.backend_sirhgp.model.Action;
import com.example.backend_sirhgp.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PermissionMapper {

    @Mapping(target = "actions",      expression = "java(mapActionsToSummary(permission.getActions()))")
    @Mapping(target = "nombreRoles",  ignore = true)
    @Mapping(target = "nombrePostes", ignore = true)
    PermissionResponseDTO toResponseDTO(Permission permission);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "actions",          ignore = true)
    @Mapping(target = "roles",            ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Permission entity, String name, String description);

    default Set<PermissionResponseDTO.ActionSummaryDTO> mapActionsToSummary(Set<Action> actions) {
        if (actions == null) return null;
        return actions.stream().map(a -> {
            PermissionResponseDTO.ActionSummaryDTO dto = new PermissionResponseDTO.ActionSummaryDTO();
            dto.setId(a.getId());
            dto.setName(a.getName());
            return dto;
        }).collect(Collectors.toSet());
    }
}