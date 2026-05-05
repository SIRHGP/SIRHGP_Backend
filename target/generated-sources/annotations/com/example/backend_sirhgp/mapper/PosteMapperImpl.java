package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.PosteResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class PosteMapperImpl implements PosteMapper {

    @Override
    public PosteResponseDTO toResponseDTO(Poste poste) {
        if ( poste == null ) {
            return null;
        }

        PosteResponseDTO posteResponseDTO = new PosteResponseDTO();

        posteResponseDTO.setRoleId( posteRoleId( poste ) );
        posteResponseDTO.setRoleName( posteRoleName( poste ) );
        posteResponseDTO.setDepartementId( posteDepartementId( poste ) );
        posteResponseDTO.setDepartementName( posteDepartementName( poste ) );
        posteResponseDTO.setId( poste.getId() );
        posteResponseDTO.setName( poste.getName() );
        posteResponseDTO.setDescription( poste.getDescription() );
        posteResponseDTO.setDateCreation( poste.getDateCreation() );
        posteResponseDTO.setDateModification( poste.getDateModification() );

        posteResponseDTO.setPermissions( mapPermissionsToSummary(poste.getPermissions()) );

        return posteResponseDTO;
    }

    @Override
    public void updateEntity(Poste entity, String name, String description) {
        if ( name == null && description == null ) {
            return;
        }

        entity.setName( name );
        entity.setDescription( description );
    }

    private Long posteRoleId(Poste poste) {
        if ( poste == null ) {
            return null;
        }
        Role role = poste.getRole();
        if ( role == null ) {
            return null;
        }
        Long id = role.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String posteRoleName(Poste poste) {
        if ( poste == null ) {
            return null;
        }
        Role role = poste.getRole();
        if ( role == null ) {
            return null;
        }
        String name = role.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long posteDepartementId(Poste poste) {
        if ( poste == null ) {
            return null;
        }
        Departement departement = poste.getDepartement();
        if ( departement == null ) {
            return null;
        }
        Long id = departement.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String posteDepartementName(Poste poste) {
        if ( poste == null ) {
            return null;
        }
        Departement departement = poste.getDepartement();
        if ( departement == null ) {
            return null;
        }
        String name = departement.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
