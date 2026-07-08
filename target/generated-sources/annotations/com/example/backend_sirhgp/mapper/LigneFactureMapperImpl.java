package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.LigneFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.LigneFactureResponseDTO;
import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.LigneFacture;
import com.example.backend_sirhgp.model.Tache;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-08T06:06:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class LigneFactureMapperImpl implements LigneFactureMapper {

    @Override
    public LigneFactureResponseDTO toResponseDTO(LigneFacture ligne) {
        if ( ligne == null ) {
            return null;
        }

        LigneFactureResponseDTO ligneFactureResponseDTO = new LigneFactureResponseDTO();

        ligneFactureResponseDTO.setFactureId( ligneFactureId( ligne ) );
        ligneFactureResponseDTO.setTacheId( ligneTacheId( ligne ) );
        ligneFactureResponseDTO.setTacheNom( ligneTacheTitre( ligne ) );
        ligneFactureResponseDTO.setId( ligne.getId() );
        ligneFactureResponseDTO.setDescription( ligne.getDescription() );
        ligneFactureResponseDTO.setQuantite( ligne.getQuantite() );
        ligneFactureResponseDTO.setPrixUnitaire( ligne.getPrixUnitaire() );
        ligneFactureResponseDTO.setMontantTotal( ligne.getMontantTotal() );

        return ligneFactureResponseDTO;
    }

    @Override
    public LigneFacture toEntity(LigneFactureRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        LigneFacture.LigneFactureBuilder ligneFacture = LigneFacture.builder();

        ligneFacture.description( dto.getDescription() );
        ligneFacture.quantite( dto.getQuantite() );
        ligneFacture.prixUnitaire( dto.getPrixUnitaire() );

        return ligneFacture.build();
    }

    @Override
    public void updateEntity(LigneFacture entity, LigneFactureRequestDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setDescription( dto.getDescription() );
        entity.setQuantite( dto.getQuantite() );
        entity.setPrixUnitaire( dto.getPrixUnitaire() );
    }

    private Long ligneFactureId(LigneFacture ligneFacture) {
        if ( ligneFacture == null ) {
            return null;
        }
        Facture facture = ligneFacture.getFacture();
        if ( facture == null ) {
            return null;
        }
        Long id = facture.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long ligneTacheId(LigneFacture ligneFacture) {
        if ( ligneFacture == null ) {
            return null;
        }
        Tache tache = ligneFacture.getTache();
        if ( tache == null ) {
            return null;
        }
        Long id = tache.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String ligneTacheTitre(LigneFacture ligneFacture) {
        if ( ligneFacture == null ) {
            return null;
        }
        Tache tache = ligneFacture.getTache();
        if ( tache == null ) {
            return null;
        }
        String titre = tache.getTitre();
        if ( titre == null ) {
            return null;
        }
        return titre;
    }
}
