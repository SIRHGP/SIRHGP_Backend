package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.CommentaireResponseDTO;
import com.example.backend_sirhgp.model.CommentaireTache;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-08T06:06:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class CommentaireMapperImpl implements CommentaireMapper {

    @Override
    public CommentaireResponseDTO toResponseDTO(CommentaireTache commentaire) {
        if ( commentaire == null ) {
            return null;
        }

        CommentaireResponseDTO commentaireResponseDTO = new CommentaireResponseDTO();

        commentaireResponseDTO.setAuteur( employerToAuteurSummaryDTO( commentaire.getAuteur() ) );
        commentaireResponseDTO.setTacheId( commentaireTacheId( commentaire ) );
        commentaireResponseDTO.setId( commentaire.getId() );
        commentaireResponseDTO.setContenu( commentaire.getContenu() );
        commentaireResponseDTO.setDateCreation( commentaire.getDateCreation() );
        commentaireResponseDTO.setDateModification( commentaire.getDateModification() );
        commentaireResponseDTO.setEstModifie( commentaire.getEstModifie() );

        return commentaireResponseDTO;
    }

    @Override
    public void updateContenu(CommentaireTache entity, String contenu) {
        if ( contenu == null ) {
            return;
        }

        entity.setContenu( contenu );
    }

    protected CommentaireResponseDTO.AuteurSummaryDTO employerToAuteurSummaryDTO(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        CommentaireResponseDTO.AuteurSummaryDTO auteurSummaryDTO = new CommentaireResponseDTO.AuteurSummaryDTO();

        auteurSummaryDTO.setId( employer.getId() );
        auteurSummaryDTO.setNom( employer.getName() );
        auteurSummaryDTO.setEmail( employer.getEmail() );

        return auteurSummaryDTO;
    }

    private Long commentaireTacheId(CommentaireTache commentaireTache) {
        if ( commentaireTache == null ) {
            return null;
        }
        Tache tache = commentaireTache.getTache();
        if ( tache == null ) {
            return null;
        }
        Long id = tache.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
