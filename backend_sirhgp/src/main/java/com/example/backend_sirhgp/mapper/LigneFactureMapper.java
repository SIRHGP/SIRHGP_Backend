package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.LigneFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.LigneFactureResponseDTO;
import com.example.backend_sirhgp.model.LigneFacture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * CORRECTION FINALE :
 * - LigneFactureResponseDTO a dateCreation et dateModification (pour l'affichage)
 *   mais LigneFacture (le modèle) n'en a pas → erreur "Unmapped target properties"
 *   sur toResponseDTO car MapStruct cherche ces champs dans la source.
 *
 * Solution : ignorer dateCreation et dateModification dans toResponseDTO également,
 * car ils n'existent pas dans le modèle LigneFacture source.
 *
 * Si tu veux ces champs dans la réponse, il faut soit les ajouter au modèle,
 * soit les retirer du LigneFactureResponseDTO.
 * La solution la plus propre : les retirer du DTO (une ligne de facture
 * n'a pas besoin de timestamps dans la réponse).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LigneFactureMapper {

    @Mapping(source = "facture.id",  target = "factureId")
    @Mapping(source = "tache.id",    target = "tacheId")
    @Mapping(source = "tache.titre", target = "tacheNom")
    @Mapping(target = "tacheCode",        ignore = true)
    // CORRECTION : dateCreation et dateModification n'existent pas dans LigneFacture
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    LigneFactureResponseDTO toResponseDTO(LigneFacture ligne);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "facture",      ignore = true)
    @Mapping(target = "tache",        ignore = true)
    @Mapping(target = "montantTotal", ignore = true)
    LigneFacture toEntity(LigneFactureRequestDTO dto);

    @Mapping(target = "id",           ignore = true)  // CORRECTION
    @Mapping(target = "facture",      ignore = true)
    @Mapping(target = "tache",        ignore = true)
    @Mapping(target = "montantTotal", ignore = true)
    void updateEntity(@MappingTarget LigneFacture entity, LigneFactureRequestDTO dto);
}