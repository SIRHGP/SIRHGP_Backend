package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.CommentaireResponseDTO;
import com.example.backend_sirhgp.model.CommentaireTache;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * CORRECTION : "id" ajouté dans les @Mapping(ignore) de updateContenu().
 * MapStruct détecte le setter setId() hérité et le signale comme non mappé.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CommentaireMapper {

    @Mapping(source = "tache.id",     target = "tacheId")
    @Mapping(source = "auteur.id",    target = "auteur.id")
    @Mapping(source = "auteur.name",  target = "auteur.nom")
    @Mapping(source = "auteur.email", target = "auteur.email")
    @Mapping(target = "auteur.photo", ignore = true)
    CommentaireResponseDTO toResponseDTO(CommentaireTache commentaire);

    // CORRECTION : id ajouté dans les champs ignorés
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "tache",            ignore = true)
    @Mapping(target = "auteur",           ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "estModifie",       ignore = true)
    void updateContenu(@MappingTarget CommentaireTache entity, String contenu);
}