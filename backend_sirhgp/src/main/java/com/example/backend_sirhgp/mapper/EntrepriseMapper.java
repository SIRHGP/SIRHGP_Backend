// ─── EntrepriseMapper.java ───────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.EntrepriseResponseDTO;
import com.example.backend_sirhgp.model.Entreprise;
import org.springframework.stereotype.Component;

@Component
public class EntrepriseMapper {

    public EntrepriseResponseDTO toResponseDTO(Entreprise entreprise) {
        if (entreprise == null) return null;
        EntrepriseResponseDTO dto = new EntrepriseResponseDTO();
        dto.setId(entreprise.getId());
        dto.setName(entreprise.getName());
        dto.setDescription(entreprise.getDescription());
        dto.setSecteurActivite(entreprise.getSecteurActivite());
        dto.setType(entreprise.getType() != null ? entreprise.getType().name() : null);
        dto.setHeureArriveeStandard(entreprise.getHeureArriveeStandard());
        dto.setHeureDepartStandard(entreprise.getHeureDepartStandard());
        dto.setHeureArriveeMinimale(entreprise.getHeureArriveeMinimale());
        dto.setAdresse(entreprise.getAdresse());
        dto.setVille(entreprise.getVille());
        dto.setTelephone(entreprise.getTelephone());
        dto.setEmail(entreprise.getEmail());
        dto.setRccm(entreprise.getRccm());
        dto.setNumeroCnpsEmployeur(entreprise.getNumeroCnpsEmployeur());
        dto.setLogoUrl(entreprise.getLogoUrl());
        dto.setStatut(entreprise.getStatut() != null ? entreprise.getStatut().name() : null);
        dto.setDateCreation(entreprise.getDateCreation());
        dto.setDateModification(entreprise.getDateModification());
        return dto;
    }
}