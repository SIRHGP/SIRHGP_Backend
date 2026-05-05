package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-05T09:21:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public DocumentResponseDTO toResponseDTO(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentResponseDTO documentResponseDTO = new DocumentResponseDTO();

        documentResponseDTO.setEmployerId( documentEmployerId( document ) );
        documentResponseDTO.setEmployerNom( documentEmployerName( document ) );
        documentResponseDTO.setExpediteurId( documentExpediteurId( document ) );
        documentResponseDTO.setExpediteurNom( documentExpediteurName( document ) );
        documentResponseDTO.setExpediteurEmail( documentExpediteurEmail( document ) );
        documentResponseDTO.setDestinataireId( documentDestinataireId( document ) );
        documentResponseDTO.setDestinataireNom( documentDestinataireName( document ) );
        documentResponseDTO.setDestinataireEmail( documentDestinataireEmail( document ) );
        documentResponseDTO.setProjetId( documentProjetId( document ) );
        documentResponseDTO.setProjetNom( documentProjetNom( document ) );
        documentResponseDTO.setId( document.getId() );
        documentResponseDTO.setNom( document.getNom() );
        documentResponseDTO.setDescription( document.getDescription() );
        documentResponseDTO.setNomFichier( document.getNomFichier() );
        documentResponseDTO.setTypeMime( document.getTypeMime() );
        documentResponseDTO.setTaille( document.getTaille() );
        documentResponseDTO.setType( document.getType() );
        documentResponseDTO.setStatus( document.getStatus() );
        documentResponseDTO.setPublique( document.isPublique() );
        documentResponseDTO.setNombreTelechargements( document.getNombreTelechargements() );
        documentResponseDTO.setMessage( document.getMessage() );
        documentResponseDTO.setEstLu( document.isEstLu() );
        documentResponseDTO.setEstArchive( document.isEstArchive() );
        documentResponseDTO.setDateCreation( document.getDateCreation() );
        documentResponseDTO.setDateEnvoi( document.getDateEnvoi() );
        documentResponseDTO.setDateLecture( document.getDateLecture() );

        enrichResponseDTO( document, documentResponseDTO );

        return documentResponseDTO;
    }

    private Long documentEmployerId(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer employer = document.getEmployer();
        if ( employer == null ) {
            return null;
        }
        Long id = employer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String documentEmployerName(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer employer = document.getEmployer();
        if ( employer == null ) {
            return null;
        }
        String name = employer.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long documentExpediteurId(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer expediteur = document.getExpediteur();
        if ( expediteur == null ) {
            return null;
        }
        Long id = expediteur.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String documentExpediteurName(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer expediteur = document.getExpediteur();
        if ( expediteur == null ) {
            return null;
        }
        String name = expediteur.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String documentExpediteurEmail(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer expediteur = document.getExpediteur();
        if ( expediteur == null ) {
            return null;
        }
        String email = expediteur.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private Long documentDestinataireId(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer destinataire = document.getDestinataire();
        if ( destinataire == null ) {
            return null;
        }
        Long id = destinataire.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String documentDestinataireName(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer destinataire = document.getDestinataire();
        if ( destinataire == null ) {
            return null;
        }
        String name = destinataire.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String documentDestinataireEmail(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer destinataire = document.getDestinataire();
        if ( destinataire == null ) {
            return null;
        }
        String email = destinataire.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private Long documentProjetId(Document document) {
        if ( document == null ) {
            return null;
        }
        Projet projet = document.getProjet();
        if ( projet == null ) {
            return null;
        }
        Long id = projet.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String documentProjetNom(Document document) {
        if ( document == null ) {
            return null;
        }
        Projet projet = document.getProjet();
        if ( projet == null ) {
            return null;
        }
        String nom = projet.getNom();
        if ( nom == null ) {
            return null;
        }
        return nom;
    }
}
