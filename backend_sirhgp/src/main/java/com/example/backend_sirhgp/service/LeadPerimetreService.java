package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Employer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Refonte LEAD 6.0 — politique de périmètre départemental du chef de département.
 *
 * <p>Point d'entrée UNIQUE des contrôles « ce LEAD peut-il agir sur cet objet ? ».
 * Avant cette refonte, chaque service refaisait son propre test
 * {@code isLead() + liste.contains(...)} avec des conventions différentes
 * (l'un traitait la liste vide comme « tout autorisé », l'autre refusait tout) :
 * c'est la source des divergences constatées entre le recrutement (déjà scopé)
 * et les autres modules (non scopés).
 *
 * <p>Règles :
 * <ul>
 *   <li>rôle ≠ LEAD → jamais bloqué ici (le rôle et le périmètre succursale
 *       restent du ressort des autres gardes) ;</li>
 *   <li>LEAD <b>avec</b> périmètre départemental → seule une cible rattachée à l'un
 *       de ses départements est acceptée ;</li>
 *   <li>LEAD <b>sans</b> ancrage départemental → repli sur son périmètre de lecture
 *       (succursale, puis entreprise) ; les gardes d'<i>écriture</i> le refusent
 *       volontairement via {@link #exigerPerimetreAncre(Long, String)} pour éviter
 *       qu'un LEAD sans département n'attribue des données à toute l'entreprise.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadPerimetreService {

    private final EntrepriseContextService entrepriseContextService;

    /** L'appelant est-il un LEAD ? Raccourci utilisé par les gardes d'écriture. */
    public boolean estLead() {
        return entrepriseContextService.isLead();
    }

    /** Départements effectivement supervisés (liste vide = pas d'ancrage). */
    public List<Long> departementsSupervises() {
        return entrepriseContextService.getScopeDepartementIds();
    }

    /**
     * Le LEAD a-t-il un périmètre départemental exploitable ? Une liste vide
     * signifie qu'il n'ancre aucun département : les écritures sur son équipe
     * deviennent impossibles à borner, donc elles sont refusées.
     */
    public boolean aPerimetreDepartemental() {
        return !departementsSupervises().isEmpty();
    }

    /**
     * Garde de lecture : l'employé est-il dans le périmètre de l'appelant ?
     * Ne lève jamais pour un rôle non LEAD (un MANAGER n'est pas bloqué ici : son
     * périmètre est la succursale, appliqué en amont par les services).
     */
    public boolean employeVisible(Long employeId) {
        if (!estLead()) return true;
        return entrepriseContextService.employeDansLePerimetre(employeId);
    }

    /** Garde d'écriture : lève si l'action du LEAD porte hors de son équipe. */
    public void exigerEmployeDansLePerimetre(Long employeId, String action) {
        if (!estLead()) return;
        if (employeVisible(employeId)) return;
        throw new AccessDeniedException(messageHorsPerimetre(action, null));
    }

    /**
     * Variante métier pour un objet déjà chargé (tâche, feuille de temps,
     * évaluation) dont l'employé cible est connu.
     */
    public void exigerEmployeDansLePerimetre(Employer employe, String action) {
        if (!estLead()) return;
        if (employe != null && employe.getId() != null && employeVisible(employe.getId())) return;
        throw new AccessDeniedException(messageHorsPerimetre(action, employe));
    }

    /**
     * Garde d'écriture la plus stricte : exige que le LEAD ait un département
     * superviseur ET que la cible y soit rattachée. Utilisée là où une erreur
     * d'attribution serait coûteuse (embauche, création d'offre, affectation
     * d'une tâche, désignation d'un évaluateur).
     */
    public void exigerPerimetreAncre(Long employeId, String action) {
        if (!estLead()) return;
        if (!aPerimetreDepartemental()) {
            throw new AccessDeniedException(
                    "Vous n'êtes chef d'aucun département : " + action
                            + " n'est possible que dans le périmètre d'un département supervisé.");
        }
        exigerEmployeDansLePerimetre(employeId, action);
    }

    /**
     * Même garde que {@link #exigerPerimetreAncre(Long, String)} pour un département
     * explicitement visé (offre d'emploi rattachée à un département).
     */
    public void exigerDepartementSupervise(Long departementId, String action) {
        if (!estLead()) return;
        if (!aPerimetreDepartemental()) {
            throw new AccessDeniedException(
                    "Vous n'êtes chef d'aucun département : " + action + " n'est pas possible.");
        }
        if (departementId == null) {
            throw new AccessDeniedException(
                    action + " exige un département de rattachement : vous n'en dirigez aucun.");
        }
        if (!entrepriseContextService.dirigeDepartement(departementId)) {
            throw new AccessDeniedException(messageHorsPerimetre(action, null));
        }
    }

    /**
     * Filtre une collection d'employés sur le périmètre du LEAD. Liste INCHANGÉE
     * pour un rôle non LEAD (le filtrage succursale reste fait en amont) ou pour
     * un LEAD sans ancrage départemental.
     */
    public List<Employer> filtrerEmployes(List<Employer> employes) {
        if (!estLead() || !aPerimetreDepartemental() || employes == null) return employes;
        return employes.stream()
                .filter(e -> e.getId() != null && employeVisible(e.getId()))
                .collect(Collectors.toList());
    }

    private String messageHorsPerimetre(String action, Employer employe) {
        String cible = (employe != null && employe.getName() != null)
                ? " '" + employe.getName() + "'"
                : "";
        return action + " : l'employé" + cible
                + " n'appartient pas à vos départements supervisés.";
    }
}