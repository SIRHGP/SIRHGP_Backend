package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.dto.response.LigneFactureResponseDTO;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.LigneFacture;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Projet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-08T06:06:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class FactureMapperImpl implements FactureMapper {

    @Autowired
    private LigneFactureMapper ligneFactureMapper;

    @Override
    public FactureResponseDTO toResponseDTO(Facture facture) {
        if ( facture == null ) {
            return null;
        }

        FactureResponseDTO factureResponseDTO = new FactureResponseDTO();

        factureResponseDTO.setEmetteur( employerToEmployerSummaryDTO( facture.getEmetteur() ) );
        factureResponseDTO.setClient( employerToEmployerSummaryDTO1( facture.getClient() ) );
        factureResponseDTO.setProjetId( factureProjetId( facture ) );
        factureResponseDTO.setProjetNom( factureProjetNom( facture ) );
        factureResponseDTO.setProjetCode( factureProjetCodeProjet( facture ) );
        factureResponseDTO.setId( facture.getId() );
        factureResponseDTO.setNumeroFacture( facture.getNumeroFacture() );
        factureResponseDTO.setTitre( facture.getTitre() );
        factureResponseDTO.setDescription( facture.getDescription() );
        factureResponseDTO.setDateEmission( facture.getDateEmission() );
        factureResponseDTO.setDateEcheance( facture.getDateEcheance() );
        factureResponseDTO.setMontantHT( facture.getMontantHT() );
        factureResponseDTO.setMontantTVA( facture.getMontantTVA() );
        factureResponseDTO.setMontantTTC( facture.getMontantTTC() );
        factureResponseDTO.setMontantPaye( facture.getMontantPaye() );
        factureResponseDTO.setMontantRestant( facture.getMontantRestant() );
        factureResponseDTO.setStatut( facture.getStatut() );
        factureResponseDTO.setTypeFacture( facture.getTypeFacture() );
        factureResponseDTO.setDateCreation( facture.getDateCreation() );
        factureResponseDTO.setDateModification( facture.getDateModification() );
        factureResponseDTO.setMessage( facture.getMessage() );
        factureResponseDTO.setEstConsultee( facture.isEstConsultee() );
        factureResponseDTO.setDateEnvoi( facture.getDateEnvoi() );
        factureResponseDTO.setDateConsultation( facture.getDateConsultation() );
        factureResponseDTO.setLignesFacture( ligneFactureSetToLigneFactureResponseDTOSet( facture.getLignesFacture() ) );

        return factureResponseDTO;
    }

    @Override
    public Facture toEntity(FactureRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Facture.FactureBuilder facture = Facture.builder();

        facture.titre( dto.getTitre() );
        facture.description( dto.getDescription() );
        facture.dateEmission( dto.getDateEmission() );
        facture.dateEcheance( dto.getDateEcheance() );
        facture.montantHT( dto.getMontantHT() );
        facture.montantTVA( dto.getMontantTVA() );
        facture.typeFacture( dto.getTypeFacture() );

        return facture.build();
    }

    @Override
    public void updateEntity(Facture entity, FactureRequestDTO dto) {
        if ( dto == null ) {
            return;
        }

        entity.setTitre( dto.getTitre() );
        entity.setDescription( dto.getDescription() );
        entity.setDateEmission( dto.getDateEmission() );
        entity.setDateEcheance( dto.getDateEcheance() );
        entity.setMontantHT( dto.getMontantHT() );
        entity.setMontantTVA( dto.getMontantTVA() );
        entity.setTypeFacture( dto.getTypeFacture() );
    }

    private String employerPosteName(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
        if ( poste == null ) {
            return null;
        }
        String name = poste.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String employerPosteDepartementName(Employer employer) {
        if ( employer == null ) {
            return null;
        }
        Poste poste = employer.getPoste();
        if ( poste == null ) {
            return null;
        }
        Departement departement = poste.getDepartement();
        if ( departement == null ) {
            return null;
        }
        String name = departement.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    protected FactureResponseDTO.EmployerSummaryDTO employerToEmployerSummaryDTO(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        FactureResponseDTO.EmployerSummaryDTO employerSummaryDTO = new FactureResponseDTO.EmployerSummaryDTO();

        employerSummaryDTO.setId( employer.getId() );
        employerSummaryDTO.setName( employer.getName() );
        employerSummaryDTO.setEmail( employer.getEmail() );
        employerSummaryDTO.setPosteName( employerPosteName( employer ) );
        employerSummaryDTO.setDepartementNom( employerPosteDepartementName( employer ) );

        return employerSummaryDTO;
    }

    protected FactureResponseDTO.EmployerSummaryDTO employerToEmployerSummaryDTO1(Employer employer) {
        if ( employer == null ) {
            return null;
        }

        FactureResponseDTO.EmployerSummaryDTO employerSummaryDTO = new FactureResponseDTO.EmployerSummaryDTO();

        employerSummaryDTO.setId( employer.getId() );
        employerSummaryDTO.setName( employer.getName() );
        employerSummaryDTO.setEmail( employer.getEmail() );
        employerSummaryDTO.setPosteName( employerPosteName( employer ) );
        employerSummaryDTO.setDepartementNom( employerPosteDepartementName( employer ) );

        return employerSummaryDTO;
    }

    private Long factureProjetId(Facture facture) {
        if ( facture == null ) {
            return null;
        }
        Projet projet = facture.getProjet();
        if ( projet == null ) {
            return null;
        }
        Long id = projet.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String factureProjetNom(Facture facture) {
        if ( facture == null ) {
            return null;
        }
        Projet projet = facture.getProjet();
        if ( projet == null ) {
            return null;
        }
        String nom = projet.getNom();
        if ( nom == null ) {
            return null;
        }
        return nom;
    }

    private String factureProjetCodeProjet(Facture facture) {
        if ( facture == null ) {
            return null;
        }
        Projet projet = facture.getProjet();
        if ( projet == null ) {
            return null;
        }
        String codeProjet = projet.getCodeProjet();
        if ( codeProjet == null ) {
            return null;
        }
        return codeProjet;
    }

    protected Set<LigneFactureResponseDTO> ligneFactureSetToLigneFactureResponseDTOSet(Set<LigneFacture> set) {
        if ( set == null ) {
            return null;
        }

        Set<LigneFactureResponseDTO> set1 = new LinkedHashSet<LigneFactureResponseDTO>( Math.max( (int) ( set.size() / .75f ) + 1, 16 ) );
        for ( LigneFacture ligneFacture : set ) {
            set1.add( ligneFactureMapper.toResponseDTO( ligneFacture ) );
        }

        return set1;
    }
}
