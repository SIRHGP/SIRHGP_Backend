package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * CORRECTION : "id" et "type" ajoutés dans les @Mapping(ignore) de updateMetadonnees().
 * MapStruct avec ReportingPolicy.ERROR signale tout champ non mappé — il faut
 * déclarer explicitement tous les champs qu'on ne touche pas lors d'un update partiel.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentMapper {

    @Mapping(source = "employer.id",    target = "employerId")
    @Mapping(source = "employer.name",  target = "employerNom")
    @Mapping(source = "uploader.id",    target = "uploaderId")
    @Mapping(source = "uploader.name",  target = "uploaderNom")
    @Mapping(source = "uploader.email", target = "uploaderEmail")
    @Mapping(source = "projet.id",      target = "projetId")
    @Mapping(source = "projet.nom",     target = "projetNom")
    @Mapping(target = "tailleFormatee", ignore = true)
    @Mapping(target = "extension",      ignore = true)
    DocumentResponseDTO toResponseDTO(Document document);

    // CORRECTION : id, type et tous les champs non modifiables explicitement ignorés
    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "type",                  ignore = true)
    @Mapping(target = "nomFichier",            ignore = true)
    @Mapping(target = "typeMime",              ignore = true)
    @Mapping(target = "cheminStockage",        ignore = true)
    @Mapping(target = "taille",               ignore = true)
    @Mapping(target = "employer",             ignore = true)
    @Mapping(target = "uploader",             ignore = true)
    @Mapping(target = "projet",               ignore = true)
    @Mapping(target = "dateCreation",         ignore = true)
    @Mapping(target = "dateModification",     ignore = true)
    @Mapping(target = "nombreTelechargements", ignore = true)
    void updateMetadonnees(@MappingTarget Document entity,
                           String nom,
                           String description,
                           boolean publique);
}