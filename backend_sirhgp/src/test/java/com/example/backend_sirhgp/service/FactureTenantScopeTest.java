package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.FactureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactureTenantScopeTest {

    @Mock private FactureRepository factureRepository;
    @Mock private EmployerRepository employerRepository;

    @InjectMocks private FactureService factureService;

    @BeforeEach
    void authenticateCompanyAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@company.test", "token"));
        Employer admin = Employer.builder()
                .id(5L)
                .email("admin@company.test")
                .entreprise(Entreprise.builder().id(42L).build())
                .role(Role.builder().name("ADMIN").build())
                .build();
        when(employerRepository.findByEmailWithRolesAndPoste("admin@company.test"))
                .thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsesTheAuthenticatedCompanyScope() {
        var pageable = PageRequest.of(0, 20);
        when(factureRepository.findByEntrepriseId(42L, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        factureService.findAll(pageable);

        verify(factureRepository).findByEntrepriseId(42L, pageable);
        verify(factureRepository, never()).findAll(pageable);
    }

    @Test
    void lookupCannotFallBackToAnotherCompaniesInvoice() {
        when(factureRepository.findByIdAndEntrepriseId(900L, 42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> factureService.findById(900L));

        verify(factureRepository).findByIdAndEntrepriseId(900L, 42L);
        verify(factureRepository, never()).findByIdWithRelations(900L);
    }
}
