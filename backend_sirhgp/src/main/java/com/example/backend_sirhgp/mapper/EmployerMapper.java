package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.EmployerCreateDTO;
import com.example.backend_sirhgp.dto.request.EmployerUpdateDTO;
import com.example.backend_sirhgp.dto.response.EmployerResponseDTO;
import com.example.backend_sirhgp.model.Employer;
import org.mapstruct.Builder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * CORRECTION : "authorities" ajouté dans les @Mapping(ignore) de toEntity() et updateEntity().
 * MapStruct voit la méthode getAuthorities() héritée de UserDetails et essaie de la mapper
 * vers une propriété "authorities" inexistante en tant que champ direct — il faut l'ignorer.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true)
)
public interface EmployerMapper {

    @Mapping(source = "poste.id",               target = "posteId")
    @Mapping(source = "poste.name",             target = "posteNom")
    @Mapping(source = "poste.departement.id",   target = "departementId")
    @Mapping(source = "poste.departement.name", target = "departementNom")
    @Mapping(source = "role.id",                target = "roleId")
    @Mapping(source = "role.name",              target = "roleName")
    @Mapping(source = "manager.id",              target = "managerId")
    @Mapping(source = "manager.name",            target = "managerName")
    @Mapping(source = "entreprise.id",           target = "entrepriseId")
    @Mapping(source = "entreprise.name",         target = "entrepriseNom")
    @Mapping(source = "sucursal.id",             target = "sucursalId")
    @Mapping(source = "sucursal.name",           target = "sucursalName")
    @Mapping(source = "poste.name",              target = "posteName")
    EmployerResponseDTO toResponseDTO(Employer employer);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "poste",            ignore = true)
    @Mapping(target = "role",             ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "actif",            ignore = true)
    @Mapping(target = "motDePasseTemporaire", ignore = true)
    @Mapping(target = "entreprise",        ignore = true)
    @Mapping(target = "sucursal",          ignore = true)
    @Mapping(target = "manager",           ignore = true)
    // CORRECTION : authorities ignoré — méthode de UserDetails, pas un champ mappable
    @Mapping(target = "authorities",      ignore = true)
    @Mapping(source = "motDePasse",       target = "motDePasse")
    Employer toEntity(EmployerCreateDTO dto);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "email",            ignore = true)
    @Mapping(target = "motDePasse",       ignore = true)
    @Mapping(target = "poste",            ignore = true)
    @Mapping(target = "role",             ignore = true)
    @Mapping(target = "dateCreation",     ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "motDePasseTemporaire", ignore = true)
    @Mapping(target = "entreprise",        ignore = true)
    @Mapping(target = "sucursal",          ignore = true)
    @Mapping(target = "manager",           ignore = true)
    // CORRECTION : authorities ignoré
    @Mapping(target = "authorities",      ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Employer entity, EmployerUpdateDTO dto);
}
