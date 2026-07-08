package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DepartementResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-08T06:06:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class DepartementMapperImpl implements DepartementMapper {

    @Override
    public DepartementResponseDTO toResponseDTO(Departement departement) {
        if ( departement == null ) {
            return null;
        }

        DepartementResponseDTO departementResponseDTO = new DepartementResponseDTO();

        departementResponseDTO.setId( departement.getId() );
        departementResponseDTO.setName( departement.getName() );
        departementResponseDTO.setDateCreation( departement.getDateCreation() );
        departementResponseDTO.setDateModification( departement.getDateModification() );

        departementResponseDTO.setPostes( mapPostesToSummary(departement.getPostes()) );

        return departementResponseDTO;
    }

    @Override
    public void updateEntity(Departement entity, String name) {
        if ( name == null ) {
            return;
        }

        entity.setName( name );
    }
}
