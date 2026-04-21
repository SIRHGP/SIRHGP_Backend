package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.RoleResponseDTO;
import com.example.backend_sirhgp.model.Role;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T11:29:20+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class RoleMapperImpl implements RoleMapper {

    @Override
    public RoleResponseDTO toResponseDTO(Role role) {
        if ( role == null ) {
            return null;
        }

        RoleResponseDTO roleResponseDTO = new RoleResponseDTO();

        roleResponseDTO.setId( role.getId() );
        roleResponseDTO.setName( role.getName() );
        roleResponseDTO.setDescription( role.getDescription() );
        roleResponseDTO.setDateCreation( role.getDateCreation() );
        roleResponseDTO.setDateModification( role.getDateModification() );

        roleResponseDTO.setPermissions( mapPermissionsToSummary(role.getPermissions()) );

        return roleResponseDTO;
    }

    @Override
    public void updateEntity(Role entity, String name, String description) {
        if ( name == null && description == null ) {
            return;
        }

        entity.setName( name );
        entity.setDescription( description );
    }
}
