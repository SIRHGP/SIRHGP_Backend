package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Politique commune aux tâches et à leurs commentaires, sans exception de rôle pour le privé. */
@Service
public class TacheAccessService {
    public boolean peutVoir(Tache tache, Employer user) {
        if (tache == null || user == null || user.getId() == null) return false;
        boolean superAdmin = user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName());
        Long entrepriseId = tache.getEntreprise() == null ? null : tache.getEntreprise().getId();
        Long userEntrepriseId = user.getEntreprise() == null ? null : user.getEntreprise().getId();
        if (!superAdmin && !Objects.equals(entrepriseId, userEntrepriseId)) return false;
        return tache.getVisibilite() != Tache.Visibilite.PRIVEE
                || (tache.getCreateur() != null && user.getId().equals(tache.getCreateur().getId()))
                || (tache.getAssignee() != null && user.getId().equals(tache.getAssignee().getId()));
    }

    public void verifierLecture(Tache tache, Employer user) {
        if (!peutVoir(tache, user)) {
            throw new AccessDeniedException("Vous ne pouvez pas consulter cette tâche.");
        }
    }
}
