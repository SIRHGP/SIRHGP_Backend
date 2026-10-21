// ─── RecrutementMappers.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.OffreEmploiRequestDTO;
import com.example.backend_sirhgp.dto.response.CandidatureResponseDTO;
import com.example.backend_sirhgp.dto.response.OffreEmploiResponseDTO;
import com.example.backend_sirhgp.model.Candidature;
import com.example.backend_sirhgp.model.OffreEmploi;
import org.springframework.stereotype.Component;

/**
 * Mappers du module Recrutement (2.2) — manuels (pattern EntrepriseMapper)
 * pour éviter la complexité MapStruct sur les relations lazy.
 */
@Component
public class RecrutementMappers {

    public OffreEmploiResponseDTO toResponseDTO(OffreEmploi o) {
        if (o == null) return null;
        OffreEmploiResponseDTO dto = new OffreEmploiResponseDTO();
        dto.setId(o.getId());
        dto.setTitre(o.getTitre());
        dto.setDescription(o.getDescription());
        dto.setTypeContrat(o.getTypeContrat());
        dto.setLieu(o.getLieu());
        dto.setStatut(o.getStatut());
        if (o.getPoste() != null) {
            dto.setPosteId(o.getPoste().getId());
            dto.setPosteName(o.getPoste().getName());
        }
        if (o.getDepartement() != null) {
            dto.setDepartementId(o.getDepartement().getId());
            dto.setDepartementNom(o.getDepartement().getName());
        }
        // 5.9 : rattachement sucursale — d'où vient l'offre (visibilité admin).
        if (o.getSucursal() != null) {
            dto.setSucursalId(o.getSucursal().getId());
            dto.setSucursalNom(o.getSucursal().getName());
        }
        dto.setDatePublication(o.getDatePublication());
        dto.setDateCloture(o.getDateCloture());
        return dto;
    }

    public CandidatureResponseDTO toResponseDTO(Candidature c) {
        if (c == null) return null;
        CandidatureResponseDTO dto = new CandidatureResponseDTO();
        dto.setId(c.getId());
        if (c.getOffre() != null) {
            dto.setOffreId(c.getOffre().getId());
            dto.setOffreTitre(c.getOffre().getTitre());
        }
        dto.setNomCandidat(c.getNomCandidat());
        dto.setEmail(c.getEmail());
        dto.setTelephone(c.getTelephone());
        dto.setCvUrl(c.getCvUrl());
        dto.setLettreMotivation(c.getLettreMotivation());
        dto.setStatut(c.getStatut());
        dto.setDateCandidature(c.getDateCandidature());
        dto.setCommentaire(c.getCommentaire());
        // 4.3 — pré-évaluation & embauche
        dto.setDocumentId(c.getDocumentId());
        dto.setDecision(c.getDecision());
        dto.setNoteEvaluation(c.getNoteEvaluation());
        dto.setAvisEvaluation(c.getAvisEvaluation());
        dto.setDateEmbauche(c.getDateEmbauche());
        if (c.getEmploye() != null) {
            dto.setEmployeId(c.getEmploye().getId());
            dto.setEmployeName(c.getEmploye().getName());
        }
        return dto;
    }
}