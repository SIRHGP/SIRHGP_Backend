// ─── SucursalAttributionService.java ─────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Sucursal;
import com.example.backend_sirhgp.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 5.9 bis — attribution de la succursale d'ORIGINE des enregistrements métiers
 * (factures, documents, projets, tâches, événements, offres d'emploi).
 *
 * La succursale d'un enregistrement ne détermine pas qui peut le voir (ça reste
 * le rôle : ADMIN = entreprise entière, plusieurs agences visibles) mais sert à
 * l'ADMIN qui pilote plusieurs succursales pour **filtrer / trier / attribuer** —
 * exactement le rôle de `Employer.sucursal` (règle RG « scoping fin », 3.4).
 *
 * Règles (calquées sur {@code EmployerService.resolveSucursal}) :
 * <ul>
 *   <li>compte ancré sur une succursale ({@code getScopeSucursalId() != null} :
 *       MANAGER / LEAD) → **sa** succursale, contrainte dure : il ne peut ni créer
 *       pour une autre agence ni re-rattacher une donnée existante ;</li>
 *   <li>ADMIN / SUPER_ADMIN (périmètre entreprise / plateforme) → la succursale
 *       **demandée** dans la requête, contrôlée sur l'entreprise ; à défaut celle
 *       du créateur s'il est rattaché (comportement 5.9 inchangé) ;</li>
 *   <li>aucune des deux → {@code NULL} = donnée du **siège** (badge « Siège » côté UI).</li>
 * </ul>
 * En modification, {@code sucursalId == null} signifie « attribution inchangée »
 * (convention PATCH partiel déjà utilisée pour les employés).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SucursalAttributionService {

    private final EntrepriseContextService entrepriseContextService;
    private final SucursalRepository sucursalRepository;

    /**
     * À la création : succursale demandée par un compte non scopé (ADMIN /
     * SUPER_ADMIN), sinon celle du créateur si rattaché, sinon NULL (siège).
     *
     * @param sucursalIdDemandee `sucursalId` du DTO d'entrée (nullable)
     * @param entreprise         entreprise de l'enregistrement en cours de création
     */
    public Sucursal resoudreCreation(Long sucursalIdDemandee, Entreprise entreprise) {
        // MANAGER / LEAD scopé : contrainte dure sur sa propre succursale.
        Long scopeSucursalId = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursalId != null) {
            if (sucursalIdDemandee != null && !scopeSucursalId.equals(sucursalIdDemandee)) {
                throw new IllegalArgumentException(
                        "Vous ne pouvez rattacher cette donnée qu'à votre propre succursale");
            }
            return sucursalRepository.findById(scopeSucursalId).orElse(null);
        }
        if (sucursalIdDemandee == null) return sucursalDuCreateur();
        return validerSuccursale(sucursalIdDemandee, entreprise);
    }

    /**
     * En modification : {@code null} = attribution conservée. Un compte scopé ne
     * peut pas déplacer une donnée vers une autre agence.
     *
     * @param attributionActuelle valeur déjà enregistrée sur l'entité
     */
    public Sucursal resoudreModification(Long sucursalIdDemandee, Sucursal attributionActuelle,
                                         Entreprise entreprise) {
        if (sucursalIdDemandee == null) return attributionActuelle;
        return validerSuccursale(sucursalIdDemandee, entreprise);
    }

    /** Succursale du créateur connecté (NULL = compte non rattaché = siège). */
    public Sucursal sucursalDuCreateur() {
        Employer createur = entrepriseContextService.getCurrentEmployer();
        return createur != null ? createur.getSucursal() : null;
    }

    // ─── interne ──────────────────────────────────────────────────────────────

    private Sucursal validerSuccursale(Long sucursalId, Entreprise entreprise) {
        Long scopeSucursalId = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursalId != null && !scopeSucursalId.equals(sucursalId)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez rattacher cette donnée qu'à votre propre succursale");
        }
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Succursale non trouvée : " + sucursalId));

        Entreprise entrepriseCible = entreprise != null
                ? entreprise
                : entrepriseContextService.getEntrepriseOfCurrentUser();
        if (sucursal.getEntreprise() != null && entrepriseCible != null
                && !sucursal.getEntreprise().getId().equals(entrepriseCible.getId())) {
            throw new IllegalArgumentException(
                    "La succursale doit appartenir à la même entreprise que l'enregistrement");
        }
        return sucursal;
    }
}
