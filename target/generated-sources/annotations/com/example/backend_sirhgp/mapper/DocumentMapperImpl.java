package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-13T11:29:20+0200",
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
        documentResponseDTO.setUploaderId( documentUploaderId( document ) );
        documentResponseDTO.setUploaderNom( documentUploaderName( document ) );
        documentResponseDTO.setUploaderEmail( documentUploaderEmail( document ) );
        documentResponseDTO.setProjetId( documentProjetId( document ) );
        documentResponseDTO.setProjetNom( documentProjetNom( document ) );
        documentResponseDTO.setId( document.getId() );
        documentResponseDTO.setNom( document.getNom() );
        documentResponseDTO.setDescription( document.getDescription() );
        documentResponseDTO.setType( document.getType() );
        documentResponseDTO.setPublique( document.isPublique() );
        documentResponseDTO.setNomFichier( document.getNomFichier() );
        documentResponseDTO.setTypeMime( document.getTypeMime() );
        documentResponseDTO.setTaille( document.getTaille() );
        documentResponseDTO.setDateCreation( document.getDateCreation() );
        documentResponseDTO.setDateModification( document.getDateModification() );
        documentResponseDTO.setNombreTelechargements( document.getNombreTelechargements() );

        return documentResponseDTO;
    }

    @Override
    public void updateMetadonnees(Document entity, String nom, String description, boolean publique) {
        if ( nom == null && description == null ) {
            return;
        }

        entity.setNom( nom );
        entity.setDescription( description );
        entity.setPublique( publique );
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

    private Long documentUploaderId(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer uploader = document.getUploader();
        if ( uploader == null ) {
            return null;
        }
        Long id = uploader.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String documentUploaderName(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer uploader = document.getUploader();
        if ( uploader == null ) {
            return null;
        }
        String name = uploader.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String documentUploaderEmail(Document document) {
        if ( document == null ) {
            return null;
        }
        Employer uploader = document.getUploader();
        if ( uploader == null ) {
            return null;
        }
        String email = uploader.getEmail();
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
