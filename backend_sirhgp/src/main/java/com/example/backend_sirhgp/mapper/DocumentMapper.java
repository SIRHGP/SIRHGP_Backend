package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.model.Document;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(source = "employer.id", target = "employerId")
    @Mapping(source = "employer.name", target = "employerNom")
    @Mapping(source = "expediteur.id", target = "expediteurId")
    @Mapping(source = "expediteur.name", target = "expediteurNom")
    @Mapping(source = "expediteur.email", target = "expediteurEmail")
    @Mapping(source = "destinataire.id", target = "destinataireId")
    @Mapping(source = "destinataire.name", target = "destinataireNom")
    @Mapping(source = "destinataire.email", target = "destinataireEmail")
    @Mapping(source = "projet.id", target = "projetId")
    @Mapping(source = "projet.nom", target = "projetNom")
    @Mapping(target = "tailleFormatee", ignore = true)
    @Mapping(target = "extension", ignore = true)
    DocumentResponseDTO toResponseDTO(Document document);

    @AfterMapping
    default void enrichResponseDTO(Document document, @MappingTarget DocumentResponseDTO dto) {
        if (document.getTaille() != null) {
            dto.setTailleFormatee(FileUtils.byteCountToDisplaySize(document.getTaille()));
        }
        dto.setExtension(FilenameUtils.getExtension(document.getNomFichier()));
    }
}