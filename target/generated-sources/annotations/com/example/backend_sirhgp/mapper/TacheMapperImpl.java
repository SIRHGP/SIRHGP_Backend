package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.TacheRequestDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.model.Tache;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class TacheMapperImpl implements TacheMapper {

    @Override
    public TacheResponseDTO toResponseDTO(Tache tache) {
        if ( tache == null ) {
            return null;
        }

        TacheResponseDTO tacheResponseDTO = new TacheResponseDTO();

        tacheResponseDTO.setAssignee( employerToEmployerSummaryDTO( tache.getAssignee() ) );
        tacheResponseDTO.setCreateur( employerToEmployerSummaryDTO1( tache.getCreateur() ) );
        tacheResponseDTO.setProjet( projetToProjetSummaryDTO( tache.getProjet() ) );
        tacheResponseDTO.setId( tache.getId() );
        tacheResponseDTO.setTitre( tache.getTitre() );
        tacheResponseDTO.setDescription( tache.getDescription() );
        tacheResponseDTO.setStatut( tache.getStatut() );
        tacheResponseDTO.setPriorite( tache.getPriorite() );
        tacheResponseDTO.setDateEcheance( tache.getDateEcheance() );
        tacheResponseDTO.setDateDebut( tache.getDateDebut() );
        tacheResponseDTO.setDateFin( tache.getDateFin() );
        tacheResponseDTO.setDateCreation( tache.getDateCreation() );
        tacheResponseDTO.setDateModification( tache.getDateModification() );

        return tacheResponseDTO;
    }

    @Override
    public Tache toEntity(TacheRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Tache.TacheBuilder tache = Tache.builder();

        tache.titre( dto.getTitre() );
        tache.description( dto.getDescription() );
        tache.statut( dto.getStatut() );
        tache.priorite( dto.getPriorite() );
        tache.dateEcheance( dto.getDateEcheance() );

        return tache.build();
    }

    @Override
    public void updateEntity(Tache entity, TacheRequestDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setTitre( dto.getTitre() );
        entity.setDescription( dto.getDescription() );
        entity.setStatut( dto.getStatut() );
        entity.setPriorite( dto.getPriorite() );
        entity.setDateEcheance( dto.getDateEcheance() );
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

    protected ProjetResponseDTO.EmployerSummaryDTO employerToEmployerSummaryDTO(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        ProjetResponseDTO.EmployerSummaryDTO employerSummaryDTO = new ProjetResponseDTO.EmployerSummaryDTO();

        employerSummaryDTO.setId( employer.getId() );
        employerSummaryDTO.setName( employer.getName() );
        employerSummaryDTO.setEmail( employer.getEmail() );
        employerSummaryDTO.setPosteName( employerPosteName( employer ) );
        employerSummaryDTO.setDepartementNom( employerPosteDepartementName( employer ) );

        return employerSummaryDTO;
    }

    protected ProjetResponseDTO.EmployerSummaryDTO employerToEmployerSummaryDTO1(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        ProjetResponseDTO.EmployerSummaryDTO employerSummaryDTO = new ProjetResponseDTO.EmployerSummaryDTO();

        employerSummaryDTO.setId( employer.getId() );
        employerSummaryDTO.setName( employer.getName() );
        employerSummaryDTO.setEmail( employer.getEmail() );
        employerSummaryDTO.setPosteName( employerPosteName( employer ) );
        employerSummaryDTO.setDepartementNom( employerPosteDepartementName( employer ) );

        return employerSummaryDTO;
    }

    protected TacheResponseDTO.ProjetSummaryDTO projetToProjetSummaryDTO(Projet projet) {
        if ( projet == null ) {
            return null;
        }

        TacheResponseDTO.ProjetSummaryDTO projetSummaryDTO = new TacheResponseDTO.ProjetSummaryDTO();

        projetSummaryDTO.setId( projet.getId() );
        projetSummaryDTO.setNom( projet.getNom() );
        projetSummaryDTO.setCodeProjet( projet.getCodeProjet() );

        return projetSummaryDTO;
    }
}
