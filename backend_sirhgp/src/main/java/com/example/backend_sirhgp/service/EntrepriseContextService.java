// ─── EntrepriseContextService.java ───────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.EntrepriseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service de contexte multi-tenant.
 *
 * Fournit l'entreprise de l'utilisateur connecté afin de scopé les requêtes :
 * - SUPER_ADMIN : transverse (peut voir toutes les entreprises) ;
 * - autres rôles : uniquement leur propre entreprise.
 *
 * 5.9 : expose également les périmètres fins utilisés par les lecture scopées —
 * succursale (MANAGER) et départements (LEAD).
 */
@Slf4j
@Service
public class EntrepriseContextService {

    private final EmployerRepository employerRepository;
    private final DepartementRepository departementRepository;
    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseContextService() {
        this(null, null, null);
    }

    @Autowired
    public EntrepriseContextService(EmployerRepository employerRepository,
                                   DepartementRepository departementRepository,
                                   EntrepriseRepository entrepriseRepository) {
        this.employerRepository = employerRepository;
        this.departementRepository = departementRepository;
        this.entrepriseRepository = entrepriseRepository;
    }

    public EntrepriseContextService(EmployerRepository employerRepository,
                                   DepartementRepository departementRepository) {
        this(employerRepository, departementRepository, null);
    }

    /** Récupère l'employé courant (authentifié) depuis le contexte de sécurité. */
    public Employer getCurrentEmployer() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return employerRepository.findByEmail(auth.getName()).orElse(null);
    }

    /** Retourne l'entreprise de l'utilisateur courant (peut être null pour un SUPER_ADMIN). */
    public Entreprise getEntrepriseOfCurrentUser() {
        Employer current = getCurrentEmployer();
        return current != null ? current.getEntreprise() : null;
    }

    /**
     * Retourne l'entreprise identifiée par l'ID, ou lance une exception si introuvable.
     */
    public Entreprise getRequiredEntreprise(Long entrepriseId) {
        if (entrepriseId == null) {
            return null;
        }
        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable : " + entrepriseId));
    }

    /** Indique si l'utilisateur courant est SUPER_ADMIN (accès transverse). */
    public boolean isSuperAdmin() {
        Employer current = getCurrentEmployer();
        if (current == null || current.getRole() == null) return false;
        return "SUPER_ADMIN".equals(current.getRole().getName());
    }

    /** Rôle courant de l'utilisateur authentifié (ou null). */
    public String currentRole() {
        Employer current = getCurrentEmployer();
        if (current == null || current.getRole() == null) return null;
        return current.getRole().getName();
    }

    /** LEAD : superviseur d'équipe (entreprise / sucursale). */
    public boolean isLead() {
        return "LEAD".equals(currentRole());
    }

    /** ADMIN, MANAGER ou LEAD : profil de gestion opérationnelle du périmètre. */
    public boolean hasRoleGestion() {
        String r = currentRole();
        return "ADMIN".equals(r) || "MANAGER".equals(r) || "LEAD".equals(r);
    }

    /**
     * Retourne l'identifiant d'entreprise à utiliser pour le scoping des requêtes.
     * - SUPER_ADMIN → null (pas de filtre : voit tout) ;
     * - autres → l'id de leur entreprise (peut être null si données historiques non rattachées).
     */
    public Long getScopeEntrepriseId() {
        if (isSuperAdmin()) return null;
        Entreprise e = getEntrepriseOfCurrentUser();
        return e != null ? e.getId() : null;
    }

    /**
     * 3.4 : sucursal de rattachement de l'utilisateur courant pour le scoping fin.
     * - SUPER_ADMIN ou ADMIN général (sans sucursal) → null (voit toute l'entreprise) ;
     * - ADMIN / MANAGER scopé à une sucursal → l'id de la sucursal.
     */
    public Long getScopeSucursalId() {
        Employer current = getCurrentEmployer();
        if (current == null || current.getSucursal() == null) return null;
        // Scoping fin par sucursale : MANAGER / LEAD sont scopés à leur sucursale.
        // ADMIN (et SUPER_ADMIN) voient toute l'entreprise → scope sucursal = null.
        String role = current.getRole() != null ? current.getRole().getName() : null;
        if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) return null;
        return current.getSucursal().getId();
    }

    /**
     * 5.9 (RG-M2-13 / P1-10) : périmètre départemental de l'utilisateur courant.
     *
     * Réservé au rôle LEAD (le MANAGER est scopé à sa succursale, l'ADMIN à son
     * entreprise). Le périmètre est l'union :
     *   - des départements dont l'employé est le chef (`Departement.superviseur`) ;
     *   - du département de son poste (`Employer.poste.departement`).
     *
     * Les deux requêtes sont scalaires/légères : aucun accès paresseux n'est
     * déclenché à partir de l'entité détachée renvoyée par `findByEmail`.
     *
     * Retourne une liste **vide** quand aucun département d'ancrage n'existe : les
     * appelants retombent alors sur le périmètre entreprise (même convention que
     * {@link #getScopeSucursalId()} qui retourne null = toute l'entreprise).
     */
    public List<Long> getScopeDepartementIds() {
        if (!isLead()) return List.of();
        Employer current = getCurrentEmployer();
        if (current == null || current.getId() == null) return List.of();

        Set<Long> ids = new LinkedHashSet<>(
                departementRepository.findIdsBySuperviseurId(current.getId()));
        Long departementDuPoste = employerRepository
                .findDepartementIdOfPoste(current.getId()).orElse(null);
        if (departementDuPoste != null) ids.add(departementDuPoste);
        return new ArrayList<>(ids);
    }

    // ─── Refonte LEAD 6.0 : périmètre Départemental exploité par les modules métier ────
    // Upstream, `getScopeDepartementIds()` n'était consommé que par l'audit. Il devient
    // la clé de voûte du périmètre de supervision : employés, feuilles de temps,
    // évaluations, tâches, événements et documents. Les helpers ci-dessous éviteront
    // de recopier le pattern « isLead() + liste vide → repli » dans chaque service.

    /**
     * Vrai si l'appelant (un LEAD) supervise le département donné.
     * Un LEAD sans ancrage départemental (liste vide) supervise « tout son périmètre
     * de repli » : la méthode renvoie alors {@code true} par convention, afin que les
     * appelants interprètent correctement le repli (identique au null de
     * {@link #getScopeSucursalId()}).
     */
    public boolean dirigeDepartement(Long departementId) {
        if (!isLead()) return false;
        if (departementId == null) return false;
        List<Long> scope = getScopeDepartementIds();
        return scope.isEmpty() || scope.contains(departementId);
    }

    /**
     * Le département de l'employé passé en argument, ou {@code null} s'il n'est pas
     * rattaché (employé sans poste, ou poste sans département). Requête scalaire :
     * aucun accès paresseux sur une entité détachée.
     */
    public Long getDepartementIdDe(Long employeId) {
        if (employeId == null) return null;
        return employerRepository.findDepartementIdOfPoste(employeId).orElse(null);
    }

    /**
     * L'employé appartient-il au périmètre du LEAD appelant ?
     * <ul>
     *   <li>rôle ≠ LEAD → {@code true} (ce n'est pas à cette méthode de dire si un
     *       MANAGER a le droit : son périmètre est la succursale, cf.
     *       {@link #getScopeSucursalId()}) ;</li>
     *   <li>LEAD avec périmètre départemental → vraie si le département de l'employé
     *       y figure ;</li>
     *   <li>LEAD sans ancrage (liste vide) → {@code true} = repli sur son périmètre
     *       succursale puis entreprise, par convention ;</li>
     *   <li>employé non rattaché (sans poste / poste sans département) → {@code true}
     *       pour la même raison : il n'est filtrable par aucun département, donc il
     *       n'est pas « hors périmètre ». Les gardes d'écriture doivent rester
     *       explicites sur ce cas (cf. LeadPerimetreService).</li>
     * </ul>
     */
    public boolean employeDansLePerimetre(Long employeId) {
        if (!isLead()) return true;
        Long departementId = getDepartementIdDe(employeId);
        if (departementId == null) return true;
        List<Long> scope = getScopeDepartementIds();
        return scope.isEmpty() || scope.contains(departementId);
    }

}