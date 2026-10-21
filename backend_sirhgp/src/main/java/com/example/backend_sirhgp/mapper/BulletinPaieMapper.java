// ─── BulletinPaieMapper.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.BulletinPaieResponseDTO;
import com.example.backend_sirhgp.model.BulletinPaie;
import org.springframework.stereotype.Component;

@Component
public class BulletinPaieMapper {

    public BulletinPaieResponseDTO toResponseDTO(BulletinPaie b) {
        if (b == null) return null;
        BulletinPaieResponseDTO dto = new BulletinPaieResponseDTO();
        dto.setId(b.getId());
        dto.setEmployeId(b.getEmploye() != null ? b.getEmploye().getId() : null);
        dto.setEmployeNom(b.getEmploye() != null ? b.getEmploye().getName() : null);
        dto.setPosteNom(b.getEmploye() != null && b.getEmploye().getPoste() != null
                ? b.getEmploye().getPoste().getName() : null);
        dto.setNui(b.getEmploye() != null ? b.getEmploye().getNui() : null);
        if (b.getEmploye() != null && b.getEmploye().getPoste() != null
                && b.getEmploye().getPoste().getDepartement() != null) {
            dto.setDepartementId(b.getEmploye().getPoste().getDepartement().getId());
            dto.setDepartementNom(b.getEmploye().getPoste().getDepartement().getName());
        }
        dto.setMois(b.getMois());
        dto.setAnnee(b.getAnnee());
        dto.setSalaireBase(b.getSalaireBase());
        dto.setHeuresTravaillees(b.getHeuresTravaillees());
        dto.setHeuresSupp(b.getHeuresSupp());
        dto.setMontantHeuresSupp(b.getMontantHeuresSupp());
        dto.setPrimes(b.getPrimes());
        dto.setIndemnites(b.getIndemnites());
        dto.setBrut(b.getBrut());
        dto.setCnpsSalariale(b.getCnpsSalariale());
        dto.setCnpsPatronale(b.getCnpsPatronale());
        dto.setIrpp(b.getIrpp());
        dto.setPvid(b.getPvid());
        dto.setCfc(b.getCfc());
        dto.setTpr(b.getTpr());
        dto.setTotalRetenues(b.getTotalRetenues());
        dto.setNetAPayer(b.getNetAPayer());
        dto.setCoutTotalEmployeur(b.getCoutTotalEmployeur());
        dto.setDateCreation(b.getDateCreation());
        return dto;
    }
}