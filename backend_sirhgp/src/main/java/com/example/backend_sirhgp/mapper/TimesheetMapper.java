// ─── TimesheetMapper.java ────────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.FeuilleTempsResponseDTO;
import com.example.backend_sirhgp.model.FeuilleTemps;
import org.springframework.stereotype.Component;

/** Mapper du module Timesheet (2.3) — manuel (pattern RecrutementMappers). */
@Component
public class TimesheetMapper {

    public FeuilleTempsResponseDTO toResponseDTO(FeuilleTemps f) {
        if (f == null) return null;
        FeuilleTempsResponseDTO dto = new FeuilleTempsResponseDTO();
        dto.setId(f.getId());
        dto.setDate(f.getDate());
        dto.setHeures(f.getHeures());
        dto.setDescription(f.getDescription());
        dto.setStatut(f.getStatut());
        if (f.getEmploye() != null) {
            dto.setEmployeId(f.getEmploye().getId());
            dto.setEmployeName(f.getEmploye().getName());
        }
        if (f.getProjet() != null) {
            dto.setProjetId(f.getProjet().getId());
            dto.setProjetNom(f.getProjet().getNom());
        }
        if (f.getTache() != null) {
            dto.setTacheId(f.getTache().getId());
            dto.setTacheTitre(f.getTache().getTitre());
        }
        return dto;
    }
}