// ─── SucursalMapper.java ────────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.SucursalResponseDTO;
import com.example.backend_sirhgp.model.Sucursal;
import org.springframework.stereotype.Component;

@Component
public class SucursalMapper {

    public SucursalResponseDTO toResponseDTO(Sucursal s) {
        if (s == null) return null;
        SucursalResponseDTO dto = new SucursalResponseDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setVille(s.getVille());
        dto.setAdresse(s.getAdresse());
        dto.setTelephone(s.getTelephone());
        dto.setStatut(s.getStatut() != null ? s.getStatut().name() : null);
        dto.setDateCreation(s.getDateCreation());
        if (s.getEntreprise() != null) {
            dto.setEntrepriseId(s.getEntreprise().getId());
            dto.setEntrepriseNom(s.getEntreprise().getName());
        }
        // 5.9 : manager de la sucursal (lazy — mapper appelé dans une transaction)
        if (s.getManager() != null) {
            dto.setManagerId(s.getManager().getId());
            dto.setManagerNom(s.getManager().getName());
        }
        return dto;
    }
}