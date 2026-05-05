package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.request.EmployerUpdateDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:41+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class EmployerMapperImpl implements EmployerMapper {

    @Override
    public EmployerResponseDTO toResponseDTO(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        EmployerResponseDTO employerResponseDTO = new EmployerResponseDTO();

        employerResponseDTO.setPosteId( employerPosteId( employer ) );
        employerResponseDTO.setPosteNom( employerPosteName( employer ) );
        employerResponseDTO.setDepartementId( employerPosteDepartementId( employer ) );
        employerResponseDTO.setDepartementNom( employerPosteDepartementName( employer ) );
        employerResponseDTO.setRoleId( employerRoleId( employer ) );
        employerResponseDTO.setRoleName( employerRoleName( employer ) );
        employerResponseDTO.setId( employer.getId() );
        employerResponseDTO.setName( employer.getName() );
        employerResponseDTO.setEmail( employer.getEmail() );
        employerResponseDTO.setDateNaiss( employer.getDateNaiss() );
        employerResponseDTO.setLieu( employer.getLieu() );
        employerResponseDTO.setRegion( employer.getRegion() );
        employerResponseDTO.setResidence( employer.getResidence() );
        employerResponseDTO.setNumeroTelephone( employer.getNumeroTelephone() );
        employerResponseDTO.setActif( employer.isActif() );
        employerResponseDTO.setDateEmbauche( employer.getDateEmbauche() );
        employerResponseDTO.setDateCreation( employer.getDateCreation() );
        employerResponseDTO.setDateModification( employer.getDateModification() );

        return employerResponseDTO;
    }

    @Override
    public Employer toEntity(EmployerCreateDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Employer employer = new Employer();

        employer.setMotDePasse( dto.getMotDePasse() );
        employer.setName( dto.getName() );
        employer.setEmail( dto.getEmail() );
        employer.setDateNaiss( dto.getDateNaiss() );
        employer.setLieu( dto.getLieu() );
        employer.setRegion( dto.getRegion() );
        employer.setResidence( dto.getResidence() );
        employer.setNumeroTelephone( dto.getNumeroTelephone() );
        employer.setDateEmbauche( dto.getDateEmbauche() );

        return employer;
    }

    @Override
    public void updateEntity(Employer entity, EmployerUpdateDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setName( dto.getName() );
        entity.setDateNaiss( dto.getDateNaiss() );
        entity.setLieu( dto.getLieu() );
        entity.setRegion( dto.getRegion() );
        entity.setResidence( dto.getResidence() );
        entity.setNumeroTelephone( dto.getNumeroTelephone() );
        entity.setDateEmbauche( dto.getDateEmbauche() );
        if ( dto.getActif() != null ) {
            entity.setActif( dto.getActif() );
        }
    }

    private Long employerPosteId(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
        if ( poste == null ) {
            return null;
        }
        Long id = poste.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String employerPosteName(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
        if ( poste == null ) {
            return null;
        }
        String name = poste.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long employerPosteDepartementId(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
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

    private String employerPosteDepartementName(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
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

    private Long employerRoleId(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Role role = employer.getRole();
        if ( role == null ) {
            return null;
        }
        Long id = role.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String employerRoleName(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Role role = employer.getRole();
        if ( role == null ) {
            return null;
        }
        String name = role.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
