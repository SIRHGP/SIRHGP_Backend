package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.EvenementRequestDTO;
import com.example.backend_sirhgp.dto.response.EvenementResponseDTO;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Evenement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EvenementMapper {

    @Mapping(source = "createur.id", target = "createurId")
    @Mapping(source = "createur.name", target = "createurNom")
    @Mapping(source = "createur.id", target = "createur.id")
    @Mapping(source = "createur.name", target = "createur.name")
    @Mapping(source = "createur.email", target = "createur.email")
    @Mapping(source = "createur.poste.name", target = "createur.posteName")
    @Mapping(source = "createur.poste.departement.name", target = "createur.departementNom")
    @Mapping(source = "projet.id", target = "projet.id")
    @Mapping(source = "projet.nom", target = "projet.nom")
    @Mapping(source = "projet.codeProjet", target = "projet.codeProjet")
    @Mapping(target = "participants", expression = "java(mapParticipantsToSummary(evenement.getParticipants()))")
    @Mapping(target = "nombreParticipants", ignore = true)
    @Mapping(target = "estPasse", ignore = true)
    @Mapping(target = "estEnCours", ignore = true)
    @Mapping(target = "estAvenir", ignore = true)
    @Mapping(target = "dureeMinutes", ignore = true)
    EvenementResponseDTO toResponseDTO(Evenement evenement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createur", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "projet", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    Evenement toEntity(EvenementRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createur", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "projet", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    void updateEntity(@MappingTarget Evenement entity, EvenementRequestDTO dto);

    default Set<EvenementResponseDTO.EmployerSummaryDTO> mapParticipantsToSummary(Set<Employer> participants) {
        if (participants == null) return null;
        return participants.stream().map(e -> {
            EvenementResponseDTO.EmployerSummaryDTO dto = new EvenementResponseDTO.EmployerSummaryDTO();
            dto.setId(e.getId());
            dto.setName(e.getName());
            dto.setEmail(e.getEmail());
            if (e.getPoste() != null) {
                dto.setPosteName(e.getPoste().getName());
                if (e.getPoste().getDepartement() != null) {
                    dto.setDepartementNom(e.getPoste().getDepartement().getName());
                }
            }
            return dto;
        }).collect(Collectors.toSet());
    }
}