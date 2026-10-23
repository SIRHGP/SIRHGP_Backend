package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.ProjetRequestDTO;
import com.example.backend_sirhgp.dto.response.ProjetResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORRECTION : "progression" ajouté dans @Mapping(ignore) de toEntity() et updateEntity().
 * Le champ progression existe dans Projet mais pas dans ProjetRequestDTO —
 * il est calculé par le service, pas fourni par le client.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProjetMapper {

    @Mapping(target = "chefProjet",    expression = "java(mapToEmployerSummary(projet.getChefProjet()))")
    @Mapping(target = "membres",       expression = "java(mapMembres(projet.getMembres()))")
    @Mapping(target = "departements",  expression = "java(mapDepartements(projet.getDepartements()))")
    @Mapping(source = "entreprise.id", target = "entrepriseId")
    @Mapping(source = "sucursal.id", target = "sucursalId")
    @Mapping(target = "nombreTaches",            ignore = true)
    @Mapping(target = "nombreTachesTerminees",   ignore = true)
    @Mapping(target = "nombreTachesEnCours",     ignore = true)
    @Mapping(target = "nombreTachesEnRetard",    ignore = true)
    @Mapping(target = "nombreFactures",          ignore = true)
    @Mapping(target = "montantTotalFactures",    ignore = true)
    @Mapping(target = "montantPayeFactures",     ignore = true)
    @Mapping(target = "montantRestantFactures",  ignore = true)
    @Mapping(target = "nombreFacturesPayees",    ignore = true)
    @Mapping(target = "nombreFacturesEnAttente", ignore = true)
    @Mapping(target = "nombreMembres",           ignore = true)
    @Mapping(target = "nombreDepartements",      ignore = true)
    @Mapping(target = "nombreEvenements",        ignore = true)
    @Mapping(target = "nombreDocuments",         ignore = true)
    @Mapping(target = "estEnRetard",             ignore = true)
    @Mapping(target = "joursRestants",           ignore = true)
    @Mapping(target = "depassementBudget",       ignore = true)
    ProjetResponseDTO toResponseDTO(Projet projet);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "chefProjet",       ignore = true)
    @Mapping(target = "membres",          ignore = true)
    @Mapping(target = "departements",     ignore = true)
    @Mapping(target = "taches",           ignore = true)
    @Mapping(target = "factures",         ignore = true)
    @Mapping(target = "evenements",       ignore = true)
    @Mapping(target = "documents",        ignore = true)
    @Mapping(target = "budgetConsomme",   ignore = true)
    @Mapping(target = "budgetRestant",    ignore = true)
    // CORRECTION : progression ignoré — calculé par le service
    @Mapping(target = "progression",      ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "entreprise",       ignore = true)
    @Mapping(target = "sucursal",         ignore = true)
    Projet toEntity(ProjetRequestDTO dto);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "chefProjet",       ignore = true)
    @Mapping(target = "membres",          ignore = true)
    @Mapping(target = "departements",     ignore = true)
    @Mapping(target = "taches",           ignore = true)
    @Mapping(target = "factures",         ignore = true)
    @Mapping(target = "evenements",       ignore = true)
    @Mapping(target = "documents",        ignore = true)
    @Mapping(target = "budgetConsomme",   ignore = true)
    @Mapping(target = "budgetRestant",    ignore = true)
    @Mapping(target = "progression",      ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "entreprise",       ignore = true)
    @Mapping(target = "sucursal",         ignore = true)
    void updateEntity(@MappingTarget Projet entity, ProjetRequestDTO dto);

    default ProjetResponseDTO.EmployerSummaryDTO mapToEmployerSummary(Employer employer) {
        if (employer == null) return null;
        ProjetResponseDTO.EmployerSummaryDTO dto = new ProjetResponseDTO.EmployerSummaryDTO();
        dto.setId(employer.getId());
        dto.setName(employer.getName());
        dto.setEmail(employer.getEmail());
        if (employer.getPoste() != null) {
            dto.setPosteName(employer.getPoste().getName());
            if (employer.getPoste().getDepartement() != null) {
                dto.setDepartementNom(employer.getPoste().getDepartement().getName());
            }
        }
        return dto;
    }

    default Set<ProjetResponseDTO.EmployerSummaryDTO> mapMembres(Set<Employer> membres) {
        if (membres == null) return null;
        return membres.stream()
                .map(this::mapToEmployerSummary)
                .collect(Collectors.toSet());
    }

    default Set<ProjetResponseDTO.DepartementSummaryDTO> mapDepartements(Set<Departement> departements) {
        if (departements == null) return null;
        return departements.stream().map(d -> {
            ProjetResponseDTO.DepartementSummaryDTO dto = new ProjetResponseDTO.DepartementSummaryDTO();
            dto.setId(d.getId());
            dto.setName(d.getName());
            return dto;
        }).collect(Collectors.toSet());
    }
}
