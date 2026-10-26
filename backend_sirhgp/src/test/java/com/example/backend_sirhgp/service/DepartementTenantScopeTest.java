package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.response.DepartementResponseDTO;
import com.example.backend_sirhgp.mapper.DepartementMapper;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartementTenantScopeTest {

    @Mock private DepartementRepository departementRepository;
    @Mock private PosteRepository posteRepository;
    @Mock private EmployerRepository employerRepository;
    @Mock private DepartementMapper departementMapper;
    @Mock private EntrepriseContextService entrepriseContextService;

    @InjectMocks private DepartementService departementService;

    @Test
    void adminReceivesOnlyDepartmentsOwnedByTheirCompany() {
        Departement own = Departement.builder().id(7L).name("Finance").build();
        Departement anotherCompany = Departement.builder().id(8L).name("Finance").build();
        when(entrepriseContextService.currentRole()).thenReturn("ADMIN");
        when(entrepriseContextService.getScopeEntrepriseId()).thenReturn(42L);
        when(entrepriseContextService.getScopeSucursalId()).thenReturn(null);
        when(departementRepository.findIdsByEntrepriseId(42L)).thenReturn(List.of(7L));
        when(departementRepository.findAll()).thenReturn(List.of(own, anotherCompany));
        DepartementResponseDTO ownDto = new DepartementResponseDTO();
        ownDto.setId(7L);
        when(departementMapper.toResponseDTO(own)).thenReturn(ownDto);
        when(employerRepository.countActifsByDepartementIdAndEntrepriseId(7L, 42L)).thenReturn(3L);

        var result = departementService.findAll(Pageable.unpaged());

        assertEquals(List.of(7L), result.getContent().stream().map(DepartementResponseDTO::getId).toList());
        verify(departementRepository).findIdsByEntrepriseId(42L);
    }
}
