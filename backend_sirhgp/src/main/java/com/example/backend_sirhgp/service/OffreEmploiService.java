// ─── OffreEmploiService.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.OffreEmploiRequestDTO;
import com.example.backend_sirhgp.dto.response.OffreEmploiResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.RecrutementMappers;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.OffreEmploi;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.repository.CandidatureRepository;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.OffreEmploiRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffreEmploiService {

    private final OffreEmploiRepository offreRepository;
    private final CandidatureRepository candidatureRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final RecrutementMappers mappers;
    private final AuditLogService auditLogService;
    private final EntrepriseContextService entrepriseContextService;
    private final SucursalAttributionService sucursalAttribution;
    // Refonte LEAD 6.0 : le LEAD crée/gère les offres de SES départements.
    private final LeadPerimetreService leadPerimetre;

    @Transactional
    public OffreEmploiResponseDTO create(OffreEmploiRequestDTO dto) {
        Entreprise entreprise = entrepriseContextService.getEntrepriseOfCurrentUser();
        if (entreprise == null) {
            throw new IllegalArgumentException("Entreprise introuvable pour l'utilisateur courant");
        }

        OffreEmploi offre = OffreEmploi.builder()
                .titre(dto.getTitre().trim())
                .description(dto.getDescription())
                .typeContrat(dto.getTypeContrat())
                .lieu(dto.getLieu())
                .statut(dto.getStatut() != null ? dto.getStatut() : OffreEmploi.StatutOffre.OUVERTE)
                .entreprise(entreprise)
                .build();

        if (dto.getPosteId() != null) {
            Poste poste = posteRepository.findById(dto.getPosteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + dto.getPosteId()));
            offre.setPoste(poste);
        }
        if (dto.getDepartementId() != null) {
            Departement dept = departementRepository.findById(dto.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + dto.getDepartementId()));
            offre.setDepartement(dept);
        }

        // Refonte LEAD 6.0 : un chef de département ne peut ouvrir une offre que
        // pour un département qu'il supervise. Sans département, l'offre serait de
        // toute façon invisible pour lui (cf. findAll : idsDiriges.isEmpty() → liste
        // vide) et hors de portée de l'embauche (verifierScopeCandidature).
        leadPerimetre.exigerDepartementSupervise(
                offre.getDepartement() != null ? offre.getDepartement().getId() : null,
                "Création d'une offre d'emploi");
        if (dto.getPosteId() != null && offre.getPoste() != null
                && offre.getPoste().getDepartement() != null) {
            // Cohérence offre <-> poste : le poste doit appartenir au département visé,
            // sinon l'offre pointerait vers deux départements différents.
            if (offre.getDepartement() == null
                    || !offre.getDepartement().getId().equals(offre.getPoste().getDepartement().getId())) {
                throw new IllegalArgumentException(
                        "Le poste sélectionné n'appartient pas au département de l'offre");
            }
        }

        // 5.9 bis : succursale d'origine de l'offre — agence choisie par l'ADMIN,
        // sinon celle du créateur, sinon NULL (siège).
        offre.setSucursal(sucursalAttribution.resoudreCreation(
                dto.getSucursalId(), entreprise));

        OffreEmploi saved = offreRepository.save(offre);
        auditLogService.record(getCurrentUser(), AuditActionType.CREATE,
                "Création de l'offre d'emploi '" + saved.getTitre() + "'",
                "OffreEmploi", saved.getId(), saved.getTitre());
        log.info("Offre créée : {}", saved.getTitre());
        return enrich(mappers.toResponseDTO(saved), saved);
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponseDTO> findAll() {
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<OffreEmploi> list;
        if (entrepriseContextService.isLead()) {
            // Refonte 5.7 : LEAD = uniquement les offres des départements qu'il dirige.
            // Filtrage SQL : une offre sans département reste invisible pour un LEAD.
            var currentLead = entrepriseContextService.getCurrentEmployer();
            List<Long> idsDiriges = currentLead == null ? List.of() :
                    departementRepository.findBySuperviseurId(currentLead.getId()).stream()
                            .map(Departement::getId).collect(Collectors.toList());
            list = (scope == null || idsDiriges.isEmpty())
                    ? List.of()
                    : offreRepository.findByEntrepriseIdAndDepartementIn(scope, idsDiriges);
        } else {
            list = scope != null
                    ? offreRepository.findByEntrepriseIdWithRelations(scope)
                    : offreRepository.findAllWithRelations();
        }
        // 5.9 : scoping fin par sucursale (MANAGER/LEAD rattachés) — null-inclusif :
        // offres du siège/sans sucursale + celles de MA succursale.
        Long scopeSucursalId = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursalId != null) {
            final Long sc = scopeSucursalId;
            list = list.stream()
                    .filter(o -> o.getSucursal() == null || sc.equals(o.getSucursal().getId()))
                    .collect(Collectors.toList());
        }
        return list.stream().map(o -> enrich(mappers.toResponseDTO(o), o)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OffreEmploiResponseDTO findById(Long id) {
        OffreEmploi offre = offreRepository.findByIdWithRelations(id);
        if (offre == null) {
            throw new ResourceNotFoundException("Offre non trouvée : " + id);
        }
        // Refonte 5.7 : un LEAD ne consulte que les offres de son périmètre
        if (entrepriseContextService.isLead()) {
            var currentLead = entrepriseContextService.getCurrentEmployer();
            Long deptId = offre.getDepartement() != null ? offre.getDepartement().getId() : null;
            boolean autorise = deptId != null
                    && departementRepository.findBySuperviseurId(currentLead.getId()).stream()
                            .anyMatch(d -> d.getId().equals(deptId));
            if (!autorise) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Cette offre est hors de votre périmètre (département : "
                                + (offre.getDepartement() != null ? offre.getDepartement().getName() : "sans département") + ")");
            }
        }
        return enrich(mappers.toResponseDTO(offre), offre);
    }

    private OffreEmploiResponseDTO enrich(OffreEmploiResponseDTO dto, OffreEmploi offre) {
        // Refonte 5.7 : COUNT SQL au lieu de findByOffreId().size() (fix N+1)
        dto.setNbCandidatures(candidatureRepository.countByOffreId(offre.getId()));
        return dto;
    }

    private com.example.backend_sirhgp.model.Employer getCurrentUser() {
        return entrepriseContextService.getCurrentEmployer();
    }

    @Transactional
    public OffreEmploiResponseDTO update(Long id, OffreEmploiRequestDTO dto) {
        OffreEmploi offre = offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée : " + id));
        // Refonte LEAD 6.0 : garde sur l'offre EN COURS avant toute modification —
        // sans elle, un LEAD pouvait réécrire (et donc détourner) une offre d'un
        // autre département en supposant un departementId conforme dans le DTO.
        leadPerimetre.exigerDepartementSupervise(
                offre.getDepartement() != null ? offre.getDepartement().getId() : null,
                "Modification d'une offre d'emploi");

        offre.setTitre(dto.getTitre().trim());
        offre.setDescription(dto.getDescription());
        offre.setTypeContrat(dto.getTypeContrat());
        offre.setLieu(dto.getLieu());
        if (dto.getStatut() != null) offre.setStatut(dto.getStatut());

        if (dto.getPosteId() != null) {
            offre.setPoste(posteRepository.findById(dto.getPosteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + dto.getPosteId())));
        } else {
            offre.setPoste(null);
        }
        if (dto.getDepartementId() != null) {
            offre.setDepartement(departementRepository.findById(dto.getDepartementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé : " + dto.getDepartementId())));
        } else {
            offre.setDepartement(null);
        }

        // 5.9 bis : re-rattachement de la succursale d'origine par un ADMIN
        // (sucursalId null = attribution conservée ; refusé pour un compte scopé).
        offre.setSucursal(sucursalAttribution.resoudreModification(
                dto.getSucursalId(), offre.getSucursal(), offre.getEntreprise()));

        OffreEmploi saved = offreRepository.save(offre);
        auditLogService.record(getCurrentUser(), AuditActionType.UPDATE,
                "Modification de l'offre d'emploi '" + saved.getTitre() + "'",
                "OffreEmploi", saved.getId(), saved.getTitre());
        return enrich(mappers.toResponseDTO(saved), saved);
    }

    /** Ouvrir / suspendre / fermer une offre (date de clôture auto à la fermeture). */
    @Transactional
    public OffreEmploiResponseDTO updateStatut(Long id, OffreEmploi.StatutOffre statut) {
        OffreEmploi offre = offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée : " + id));
        leadPerimetre.exigerDepartementSupervise(
                offre.getDepartement() != null ? offre.getDepartement().getId() : null,
                "Changement de statut d'une offre d'emploi");
        offre.setStatut(statut);
        offre.setDateCloture(statut == OffreEmploi.StatutOffre.FERMEE ? LocalDateTime.now() : null);
        OffreEmploi saved = offreRepository.save(offre);
        auditLogService.record(getCurrentUser(), AuditActionType.UPDATE,
                "Statut de l'offre '" + saved.getTitre() + "' : " + statut,
                "OffreEmploi", saved.getId(), saved.getTitre());
        return enrich(mappers.toResponseDTO(saved), saved);
    }

    @Transactional
    public void delete(Long id) {
        OffreEmploi offre = offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée : " + id));
        leadPerimetre.exigerDepartementSupervise(
                offre.getDepartement() != null ? offre.getDepartement().getId() : null,
                "Suppression d'une offre d'emploi");
        auditLogService.record(getCurrentUser(), AuditActionType.DELETE,
                "Suppression de l'offre d'emploi '" + offre.getTitre() + "'",
                "OffreEmploi", offre.getId(), offre.getTitre());
        offreRepository.delete(offre);
    }
}