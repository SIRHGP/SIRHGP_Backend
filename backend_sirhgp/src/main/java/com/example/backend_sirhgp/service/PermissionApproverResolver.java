package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves the approval chain for permissions and their uploaded justifications. */
@Component
@RequiredArgsConstructor
public class PermissionApproverResolver {

    private final EmployerRepository employerRepository;
    private final DepartementRepository departementRepository;

    /** STAFF/STAGIAIRE -> LEAD; LEAD -> MANAGER; MANAGER -> ADMIN. */
    public Employer resolve(Employer requester) {
        String role = requester.getRole() != null ? requester.getRole().getName() : null;
        Long companyId = requester.getEntreprise() != null ? requester.getEntreprise().getId() : null;

        Employer candidate = switch (role == null ? "" : role) {
            case "SECRETAIRE" -> managerOfBranch(requester);
            case "STAFF", "STAGIAIRE" -> departmentLead(requester);
            case "LEAD" -> firstActive(companyId != null
                    ? employerRepository.findManagersActifsByEntrepriseId(companyId)
                    : employerRepository.findAllManagersActifs());
            case "MANAGER" -> firstActive(companyId != null
                    ? employerRepository.findAdminsByEntrepriseId(companyId)
                    : List.of());
            default -> null;
        };

        // If a link in the chain is missing, fall back to an administrator of the same company.
        if (candidate == null && companyId != null
                && !"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            candidate = employerRepository.findAdminsByEntrepriseId(companyId).stream()
                    .filter(admin -> !admin.getId().equals(requester.getId()))
                    .findFirst().orElse(null);
        }
        return candidate != null && candidate.getId().equals(requester.getId()) ? null : candidate;
    }

    private Employer managerOfBranch(Employer requester) {
        if (requester.getSucursal() == null || requester.getSucursal().getManager() == null) return null;
        Employer manager = employerRepository.findByIdWithDetails(requester.getSucursal().getManager().getId()).orElse(null);
        if (manager == null || !manager.isActif() || manager.getRole() == null
                || !"MANAGER".equals(manager.getRole().getName())) return null;
        if (manager.getEntreprise() == null || requester.getEntreprise() == null
                || !manager.getEntreprise().getId().equals(requester.getEntreprise().getId())) return null;
        if (manager.getSucursal() == null || !manager.getSucursal().getId().equals(requester.getSucursal().getId())) return null;
        return manager;
    }

    private Employer departmentLead(Employer requester) {
        if (requester.getPoste() == null || requester.getPoste().getDepartement() == null) return null;
        Long departmentId = requester.getPoste().getDepartement().getId();
        Departement department = departementRepository.findById(departmentId).orElse(null);
        if (department == null || department.getSuperviseur() == null) return null;
        Employer lead = employerRepository.findByIdWithDetails(department.getSuperviseur().getId()).orElse(null);
        if (lead == null || !lead.isActif()) return null;

        String leadRole = lead.getRole() != null ? lead.getRole().getName() : null;
        if (!"LEAD".equals(leadRole) && !"ADMIN".equals(leadRole)) return null;

        Long leadCompanyId = lead.getEntreprise() != null ? lead.getEntreprise().getId() : null;
        Long requesterCompanyId = requester.getEntreprise() != null ? requester.getEntreprise().getId() : null;
        if (requesterCompanyId != null && leadCompanyId != null && !requesterCompanyId.equals(leadCompanyId)) return null;

        Long requesterBranchId = requester.getSucursal() != null ? requester.getSucursal().getId() : null;
        Long leadBranchId = lead.getSucursal() != null ? lead.getSucursal().getId() : null;
        if (requesterBranchId != null && leadBranchId != null && !requesterBranchId.equals(leadBranchId)) return null;
        return lead;
    }

    private Employer firstActive(List<Employer> candidates) {
        return candidates != null ? candidates.stream().filter(Employer::isActif).findFirst().orElse(null) : null;
    }
}
