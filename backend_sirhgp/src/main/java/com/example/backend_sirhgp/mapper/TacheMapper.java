package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.TacheRequestDTO;
import com.example.backend_sirhgp.dto.response.TacheResponseDTO;
import com.example.backend_sirhgp.model.Tache;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TacheMapper {

    @Mapping(source = "assignee.id",                     target = "assignee.id")
    @Mapping(source = "assignee.name",                   target = "assignee.name")
    @Mapping(source = "assignee.email",                  target = "assignee.email")
    @Mapping(source = "assignee.poste.name",             target = "assignee.posteName")
    @Mapping(source = "assignee.poste.departement.name", target = "assignee.departementNom")
    @Mapping(source = "createur.id",                     target = "createur.id")
    @Mapping(source = "createur.name",                   target = "createur.name")
    @Mapping(source = "createur.email",                  target = "createur.email")
    @Mapping(source = "createur.poste.name",             target = "createur.posteName")
    @Mapping(source = "createur.poste.departement.name", target = "createur.departementNom")
    @Mapping(source = "projet.id",                       target = "projet.id")
    @Mapping(source = "projet.nom",                      target = "projet.nom")
    @Mapping(source = "projet.codeProjet",               target = "projet.codeProjet")
    @Mapping(target = "nombreCommentaires",    ignore = true)
    @Mapping(target = "commentairesRecents",   ignore = true)
    @Mapping(target = "estEnRetard",           ignore = true)
    @Mapping(target = "joursRestants",         ignore = true)
    @Mapping(target = "dureeExecution",        ignore = true)
    @Mapping(target = "pourcentageCompletion", ignore = true)
    TacheResponseDTO toResponseDTO(Tache tache);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "assignee",         ignore = true)
    @Mapping(target = "createur",         ignore = true)
    @Mapping(target = "projet",           ignore = true)
    @Mapping(target = "commentaires",     ignore = true)
    @Mapping(target = "dateDebut",        ignore = true)
    @Mapping(target = "dateFin",          ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    Tache toEntity(TacheRequestDTO dto);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "assignee",         ignore = true)
    @Mapping(target = "createur",         ignore = true)
    @Mapping(target = "projet",           ignore = true)
    @Mapping(target = "commentaires",     ignore = true)
    // CORRECTION : dateDebut et dateFin ignorés dans updateEntity aussi
    @Mapping(target = "dateDebut",        ignore = true)
    @Mapping(target = "dateFin",          ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Tache entity, TacheRequestDTO dto);
}