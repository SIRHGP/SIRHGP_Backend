package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.PermissionResponseDTO;
import com.example.backend_sirhgp.model.Permission;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public PermissionResponseDTO toResponseDTO(Permission permission) {
        if ( permission == null ) {
            return null;
        }

        PermissionResponseDTO permissionResponseDTO = new PermissionResponseDTO();

        permissionResponseDTO.setId( permission.getId() );
        permissionResponseDTO.setName( permission.getName() );
        permissionResponseDTO.setDescription( permission.getDescription() );
        permissionResponseDTO.setDateCreation( permission.getDateCreation() );
        permissionResponseDTO.setDateModification( permission.getDateModification() );

        permissionResponseDTO.setActions( mapActionsToSummary(permission.getActions()) );

        return permissionResponseDTO;
    }

    @Override
    public void updateEntity(Permission entity, String name, String description) {
        if ( name == null && description == null ) {
            return;
        }

        entity.setName( name );
        entity.setDescription( description );
    }
}
