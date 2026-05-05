package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.EvenementRequestDTO;
import com.example.backend_sirhgp.dto.response.EvenementResponseDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Evenement;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Projet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:41+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class EvenementMapperImpl implements EvenementMapper {

    @Override
    public EvenementResponseDTO toResponseDTO(Evenement evenement) {
        if ( evenement == null ) {
            return null;
        }

        EvenementResponseDTO evenementResponseDTO = new EvenementResponseDTO();

        evenementResponseDTO.setCreateur( employerToEmployerSummaryDTO( evenement.getCreateur() ) );
        evenementResponseDTO.setProjet( projetToProjetSummaryDTO( evenement.getProjet() ) );
        evenementResponseDTO.setId( evenement.getId() );
        evenementResponseDTO.setTitre( evenement.getTitre() );
        evenementResponseDTO.setDescription( evenement.getDescription() );
        evenementResponseDTO.setDateDebut( evenement.getDateDebut() );
        evenementResponseDTO.setHeureDebut( evenement.getHeureDebut() );
        evenementResponseDTO.setDateFin( evenement.getDateFin() );
        evenementResponseDTO.setHeureFin( evenement.getHeureFin() );
        evenementResponseDTO.setLieu( evenement.getLieu() );
        evenementResponseDTO.setCouleur( evenement.getCouleur() );
        evenementResponseDTO.setType( evenement.getType() );
        evenementResponseDTO.setRappelActif( evenement.isRappelActif() );
        evenementResponseDTO.setMinutesRappel( evenement.getMinutesRappel() );
        evenementResponseDTO.setJourEntier( evenement.isJourEntier() );
        evenementResponseDTO.setDateCreation( evenement.getDateCreation() );
        evenementResponseDTO.setDateModification( evenement.getDateModification() );

        evenementResponseDTO.setParticipants( mapParticipantsToSummary(evenement.getParticipants()) );

        return evenementResponseDTO;
    }

    @Override
    public Evenement toEntity(EvenementRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Evenement.EvenementBuilder evenement = Evenement.builder();

        evenement.titre( dto.getTitre() );
        evenement.description( dto.getDescription() );
        evenement.dateDebut( dto.getDateDebut() );
        evenement.heureDebut( dto.getHeureDebut() );
        evenement.dateFin( dto.getDateFin() );
        evenement.heureFin( dto.getHeureFin() );
        evenement.lieu( dto.getLieu() );
        evenement.couleur( dto.getCouleur() );
        evenement.type( dto.getType() );
        evenement.minutesRappel( dto.getMinutesRappel() );
        evenement.rappelActif( dto.isRappelActif() );
        evenement.jourEntier( dto.isJourEntier() );

        return evenement.build();
    }

    @Override
    public void updateEntity(Evenement entity, EvenementRequestDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setTitre( dto.getTitre() );
        entity.setDescription( dto.getDescription() );
        entity.setDateDebut( dto.getDateDebut() );
        entity.setHeureDebut( dto.getHeureDebut() );
        entity.setDateFin( dto.getDateFin() );
        entity.setHeureFin( dto.getHeureFin() );
        entity.setLieu( dto.getLieu() );
        entity.setCouleur( dto.getCouleur() );
        entity.setType( dto.getType() );
        entity.setMinutesRappel( dto.getMinutesRappel() );
        entity.setRappelActif( dto.isRappelActif() );
        entity.setJourEntier( dto.isJourEntier() );
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
