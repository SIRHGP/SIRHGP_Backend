package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.model.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * CORRECTION : "id" ajouté dans @Mapping(ignore) de toEntity().
 * MapStruct détecte setId() et le signale comme non mappé avec ERROR policy.
 */
@Mapper(componentModel = "spring",
        uses = {LigneFactureMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FactureMapper {

    @Mapping(source = "projet.id",         target = "projetId")
    @Mapping(source = "projet.nom",        target = "projetNom")
    @Mapping(source = "projet.codeProjet", target = "projetCode")
    @Mapping(source = "emetteur.id",    target = "emetteur.id")
    @Mapping(source = "emetteur.name",  target = "emetteur.name")
    @Mapping(source = "emetteur.email", target = "emetteur.email")
    @Mapping(source = "emetteur.poste.name",             target = "emetteur.posteName")
    @Mapping(source = "emetteur.poste.departement.name", target = "emetteur.departementNom")
    @Mapping(source = "client.id",    target = "client.id")
    @Mapping(source = "client.name",  target = "client.name")
    @Mapping(source = "client.email", target = "client.email")
    @Mapping(source = "client.poste.name",             target = "client.posteName")
    @Mapping(source = "client.poste.departement.name", target = "client.departementNom")
    @Mapping(target = "pourcentagePaye",       ignore = true)
    @Mapping(target = "estPayee",              ignore = true)
    @Mapping(target = "estEnRetard",           ignore = true)
    @Mapping(target = "joursRestants",         ignore = true)
    @Mapping(target = "delaiEmissionPaiement", ignore = true)
    FactureResponseDTO toResponseDTO(Facture facture);

    // CORRECTION : id ajouté dans les champs ignorés
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "numeroFacture",    ignore = true)
    @Mapping(target = "montantTTC",       ignore = true)
    @Mapping(target = "montantPaye",      ignore = true)
    @Mapping(target = "montantRestant",   ignore = true)
    @Mapping(target = "statut",           ignore = true)
    @Mapping(target = "projet",           ignore = true)
    @Mapping(target = "emetteur",         ignore = true)
    @Mapping(target = "client",           ignore = true)
    @Mapping(target = "lignesFacture",    ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    Facture toEntity(FactureRequestDTO dto);

    @Mapping(target = "id",              ignore = true)  // CORRECTION
    @Mapping(target = "numeroFacture",    ignore = true)
    @Mapping(target = "montantTTC",       ignore = true)
    @Mapping(target = "montantPaye",      ignore = true)
    @Mapping(target = "montantRestant",   ignore = true)
    @Mapping(target = "statut",           ignore = true)
    @Mapping(target = "projet",           ignore = true)
    @Mapping(target = "emetteur",         ignore = true)
    @Mapping(target = "client",           ignore = true)
    @Mapping(target = "lignesFacture",    ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Facture entity, FactureRequestDTO dto);
}