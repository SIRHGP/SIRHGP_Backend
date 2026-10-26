package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.DepartementResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Poste;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper pour Departement.
 *
 * CORRECTIONS :
 * - Cible = DepartementResponseDTO au lieu de l'ancien DepartementDTO.
 * - dateCreation et dateModification mappés normalement — ils existent
 *   maintenant dans le modèle corrigé (n'étaient pas ignorés intentionnellement
 *   mais parce qu'ils manquaient dans le modèle original).
 * - Compteurs (nombreEmployes, nombrePostes, nombreProjets) toujours ignorés :
 *   ils sont calculés côté service par des requêtes COUNT.
 * - toEntity() supprimé : la création d'un Departement se fait dans le service
 *   depuis DepartementRequestDTO qui est très simple (juste un name).
 * - updateEntity() conservé pour les mises à jour de champs scalaires.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DepartementMapper {


    @Mapping(target = "postes",           expression = "java(mapPostesToSummary(departement.getPostes()))")
    @Mapping(target = "nombreEmployes",   ignore = true)
    @Mapping(target = "nombrePostes",     ignore = true)
    @Mapping(target = "nombreProjets",    ignore = true)
    @Mapping(source = "superviseur.id", target = "superviseurId")
    @Mapping(source = "superviseur.name", target = "superviseurName")
    DepartementResponseDTO toResponseDTO(Departement departement);

    @Mapping(target = "id",              ignore = true)  // CORRECTION
    @Mapping(target = "postes", ignore = true)
    @Mapping(target = "superviseur", ignore = true)
    @Mapping(target = "entreprise", ignore = true)
    @Mapping(target = "dateCreation",    ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Departement entity, String name);

    default Set<DepartementResponseDTO.PosteSummaryDTO> mapPostesToSummary(Set<Poste> postes) {
        if (postes == null) return null;
        return postes.stream().map(p -> {
            DepartementResponseDTO.PosteSummaryDTO dto = new DepartementResponseDTO.PosteSummaryDTO();
            dto.setId(p.getId());
            dto.setName(p.getName());
            // nombreEmployes calculé dans le service
            return dto;
        }).collect(Collectors.toSet());
    }
}
