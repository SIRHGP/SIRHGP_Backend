package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.ActionResponseDTO;
import com.example.backend_sirhgp.model.Action;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper pour l'entité Action.
 *
 * CORRECTION : manquait complètement dans l'original alors que ActionDTO existait.
 *
 * Pas de toEntity() ici : la création d'une Action depuis un ActionRequestDTO
 * est suffisamment simple pour être faite directement dans le service
 * (Action.builder().name(...).description(...).build()).
 * Un mapper toEntity() n'apporterait aucune valeur sur une entité aussi simple.
 *
 * Note : INSTANCE supprimé partout — avec componentModel = "spring",
 * Spring gère le cycle de vie du bean. Garder INSTANCE crée une deuxième
 * instance non gérée par Spring (pas de transaction, pas d'injection).
 */
@Mapper(componentModel = "spring")
public interface ActionMapper {

    @Mapping(target = "nombrePermissions", ignore = true)
    ActionResponseDTO toResponseDTO(Action action);
}