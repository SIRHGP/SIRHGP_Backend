package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.ProjetRequestDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.model.Projet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T11:29:20+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class ProjetMapperImpl implements ProjetMapper {

    @Override
    public ProjetResponseDTO toResponseDTO(Projet projet) {
        if ( projet == null ) {
            return null;
        }

        ProjetResponseDTO projetResponseDTO = new ProjetResponseDTO();

        projetResponseDTO.setId( projet.getId() );
        projetResponseDTO.setNom( projet.getNom() );
        projetResponseDTO.setDescription( projet.getDescription() );
        projetResponseDTO.setCodeProjet( projet.getCodeProjet() );
        projetResponseDTO.setStatut( projet.getStatut() );
        projetResponseDTO.setDateDebut( projet.getDateDebut() );
        projetResponseDTO.setDateFinPrevue( projet.getDateFinPrevue() );
        projetResponseDTO.setDateFinReelle( projet.getDateFinReelle() );
        projetResponseDTO.setBudgetAlloue( projet.getBudgetAlloue() );
        projetResponseDTO.setBudgetConsomme( projet.getBudgetConsomme() );
        projetResponseDTO.setBudgetRestant( projet.getBudgetRestant() );
        projetResponseDTO.setProgression( projet.getProgression() );
        projetResponseDTO.setDateCreation( projet.getDateCreation() );
        projetResponseDTO.setDateModification( projet.getDateModification() );

        projetResponseDTO.setChefProjet( mapToEmployerSummary(projet.getChefProjet()) );
        projetResponseDTO.setMembres( mapMembres(projet.getMembres()) );
        projetResponseDTO.setDepartements( mapDepartements(projet.getDepartements()) );

        return projetResponseDTO;
    }

    @Override
    public Projet toEntity(ProjetRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Projet.ProjetBuilder projet = Projet.builder();

        projet.nom( dto.getNom() );
        projet.description( dto.getDescription() );
        projet.codeProjet( dto.getCodeProjet() );
        projet.statut( dto.getStatut() );
        projet.dateDebut( dto.getDateDebut() );
        projet.dateFinPrevue( dto.getDateFinPrevue() );
        projet.dateFinReelle( dto.getDateFinReelle() );
        projet.budgetAlloue( dto.getBudgetAlloue() );

        return projet.build();
    }

    @Override
    public void updateEntity(Projet entity, ProjetRequestDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setNom( dto.getNom() );
        entity.setDescription( dto.getDescription() );
        entity.setCodeProjet( dto.getCodeProjet() );
        entity.setStatut( dto.getStatut() );
        entity.setDateDebut( dto.getDateDebut() );
        entity.setDateFinPrevue( dto.getDateFinPrevue() );
        entity.setDateFinReelle( dto.getDateFinReelle() );
        entity.setBudgetAlloue( dto.getBudgetAlloue() );
    }
}
