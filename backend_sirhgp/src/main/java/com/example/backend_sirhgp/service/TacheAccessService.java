package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** Politique commune aux tâches et à leurs commentaires, sans exception de rôle pour le privé. */
@Service
public class TacheAccessService {
    private final EntrepriseContextService entrepriseContextService;

    public TacheAccessService(EntrepriseContextService entrepriseContextService) {
        this.entrepriseContextService = entrepriseContextService;
    }

    public boolean peutVoir(Tache tache, Employer user) {
        if (tache == null || user == null || user.getId() == null) return false;
        boolean superAdmin = user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName());
        if (superAdmin) return false;
        Long entrepriseId = tache.getEntreprise() == null ? null : tache.getEntreprise().getId();
        Long userEntrepriseId = user.getEntreprise() == null ? null : user.getEntreprise().getId();
        if (!Objects.equals(entrepriseId, userEntrepriseId)) return false;
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursal != null) {
            Long taskSucursalId = tache.getSucursal() == null ? null : tache.getSucursal().getId();
            if (!Objects.equals(taskSucursalId, scopeSucursal)) return false;
        }
        boolean createur = tache.getCreateur() != null && user.getId().equals(tache.getCreateur().getId());
        boolean assignee = tache.getAssignee() != null && user.getId().equals(tache.getAssignee().getId());
        if (tache.getVisibilite() == Tache.Visibilite.PRIVEE && !createur && !assignee) return false;

        String role = user.getRole() == null ? "" : user.getRole().getName();
        if ("SECRETAIRE".equals(role)) return createur || assignee;
        if ("LEAD".equals(role)) {
            List<Long> departements = entrepriseContextService.getScopeDepartementIds();
            if (!departements.isEmpty()) {
                boolean projetDansScope = tache.getProjet() != null && tache.getProjet().getDepartements() != null
                        && tache.getProjet().getDepartements().stream().anyMatch(d -> departements.contains(d.getId()));
                Long assigneeDept = tache.getAssignee() == null ? null : entrepriseContextService.getDepartementIdDe(tache.getAssignee().getId());
                Long createurDept = tache.getCreateur() == null ? null : entrepriseContextService.getDepartementIdDe(tache.getCreateur().getId());
                if (!projetDansScope && !departements.contains(assigneeDept) && !departements.contains(createurDept)) return false;
            }
        }
        if ("STAFF".equals(role) || "STAGIAIRE".equals(role)) {
            boolean membreProjet = tache.getProjet() != null && tache.getProjet().getMembres() != null
                    && tache.getProjet().getMembres().stream().anyMatch(e -> user.getId().equals(e.getId()));
            boolean chefProjet = tache.getProjet() != null && tache.getProjet().getChefProjet() != null
                    && user.getId().equals(tache.getProjet().getChefProjet().getId());
            return createur || assignee || membreProjet || chefProjet;
        }
        return true;
    }

    public void verifierLecture(Tache tache, Employer user) {
        if (!peutVoir(tache, user)) {
            throw new AccessDeniedException("Vous ne pouvez pas consulter cette tâche.");
        }
    }
}
