// ─── FacturationCycleService.java ────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.FacturationCycleRequestDTO;
import com.example.backend_sirhgp.dto.request.FacturationDocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.FactureRequestDTO;
import com.example.backend_sirhgp.dto.request.PaiementFactureRequestDTO;
import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.dto.response.FacturationCycleResponseDTO;
import com.example.backend_sirhgp.dto.response.FactureResponseDTO;
import com.example.backend_sirhgp.dto.response.PaiementFactureResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.FacturationCycle;
import com.example.backend_sirhgp.model.FacturationCycle.StatutCycle;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.FacturationCycleRepository;
import com.example.backend_sirhgp.repository.FactureRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dossier de facturation (refonte interface) : UNE facture = un cycle séquentiel
 * en 3 étapes — 1) PROFORMA (PDF reçu) → 2) BON DE COMMANDE (saisie + PDF généré)
 * → 3) FACTURE RÉGULIÈRE (saisie ou pièce reçue, après paiement) → clôture.
 *
 * Règle fondamentale : une facture n'existe pleinement que lorsque les 3 étapes
 * sont complétées ; toute tentative de saut d'étape est refusée (400).
 * OHADA : les Factures du cycle passent par FactureService (numérotation
 * séquentielle, écritures 411/70x/4431 pour la seule régulière).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturationCycleService {

    private final FacturationCycleRepository cycleRepository;
    private final FactureRepository factureRepository;
    private final ProjetRepository projetRepository;
    private final EmployerRepository employerRepository;
    private final com.example.backend_sirhgp.repository.DocumentRepository documentRepository;
    private final FactureService factureService;
    private final DocumentService documentService;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;

    // ─── CRUD de base ─────────────────────────────────────────────────────────

    /** Création du dossier (statut BROUILLON, étape 1 en attente de proforma). */
    @Transactional
    public FacturationCycleResponseDTO create(FacturationCycleRequestDTO dto) {
        Employer createur = entrepriseContextService.getCurrentEmployer();

        Projet projet = projetRepository.findById(dto.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));

        FacturationCycle cycle = new FacturationCycle();
        cycle.setTitre(dto.getTitre().trim());
        cycle.setDescription(dto.getDescription());
        cycle.setProjet(projet);
        cycle.setSens(dto.getSens() != null ? dto.getSens() : Facture.SensFacture.VENTE);
        cycle.setNomFournisseur(cycle.getSens() == Facture.SensFacture.ACHAT
                ? dto.getNomFournisseur() : null);
        cycle.setCommentaire(dto.getCommentaire());
        cycle.setStatut(StatutCycle.BROUILLON);
        cycle.setNumeroReference(referenceProvisoire());

        if (dto.getClientId() != null) {
            Employer client = employerRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé : " + dto.getClientId()));
            cycle.setClient(client);
        }
        Long entrepriseId = entrepriseContextService.getScopeEntrepriseId();
        if (entrepriseId != null) {
            cycle.setEntreprise(entrepriseContextService.getRequiredEntreprise(entrepriseId));
        }

        FacturationCycle saved = cycleRepository.save(cycle);
        // Référence définitive au format CYC-AAAA-NNNN (une fois l'id attribué).
        saved.setNumeroReference(String.format("CYC-%d-%04d", LocalDate.now().getYear(), saved.getId()));
        saved = cycleRepository.save(saved);

        auditLogService.record(createur, AuditActionType.CREATE,
                "Ouverture du dossier de facturation " + saved.getNumeroReference() + " — " + saved.getTitre(),
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        log.info("Dossier de facturation ouvert : {} — {}", saved.getNumeroReference(), saved.getTitre());
        return toDto(saved);
    }

    /** Liste des dossiers (scope multi-tenant), plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<FacturationCycleResponseDTO> findAll() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<FacturationCycle> list = scope != null
                ? cycleRepository.findByEntrepriseId(scope, sort)
                : cycleRepository.findAll(sort);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacturationCycleResponseDTO findById(Long id) {
        return toDto(requireCycle(id));
    }

    // ─── Étape 1 — PROFORMA (PDF reçu) ────────────────────────────────────────

    /** Étape 1 : upload du PDF du proforma — pièce de référence du cycle. */
    @Transactional
    public FacturationCycleResponseDTO uploadProforma(Long id, MultipartFile fichier) throws IOException {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getStatut() != null && cycle.getStatut().compareTo(StatutCycle.PROFORMA_RECU) > 0) {
            throw new IllegalArgumentException(
                    "Étape 1 verrouillée : le bon de commande a déjà été généré pour " + cycle.getNumeroReference());
        }
        DocumentResponseDTO doc = documentService.uploadCyclePdf(
                "Proforma — " + cycle.getNumeroReference(),
                "Étape 1/3 : proforma reçu (dossier " + cycle.getNumeroReference() + ")",
                fichier);

        Document piece = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pièce proforma non trouvée"));
        cycle.setProformaDocument(piece);
        if (cycle.getDateProforma() == null || cycle.getStatut() == StatutCycle.BROUILLON) {
            cycle.setDateProforma(java.time.LocalDateTime.now());
        }
        cycle.setStatut(StatutCycle.PROFORMA_RECU);

        FacturationCycle saved = cycleRepository.save(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Proforma reçu pour le dossier " + saved.getNumeroReference() + " (étape 1/3 validée)",
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        log.info("Étape 1 validée (proforma) pour le dossier #{}", saved.getId());
        return toDto(saved);
    }

    // ─── Étape 2 — BON DE COMMANDE (saisie + PDF généré) ──────────────────────

    /**
     * Étape 2 : construction du bon de commande depuis les champs de saisie.
     * Crée (ou régénère) la Facture BON_COMMANDE — sans écriture OHADA
     * (seule la régulière VALIDEE est comptabilisée).
     */
    @Transactional
    public FacturationCycleResponseDTO genererBonCommande(Long id, FacturationDocumentRequestDTO dto) {
        FacturationCycle cycle = requireCycle(id);
        StatutCycle statut = cycle.getStatut();
        if (statut == StatutCycle.BROUILLON) {
            throw new IllegalArgumentException(
                    "Étape 1 obligatoire : téléversez d'abord le proforma du dossier " + cycle.getNumeroReference());
        }
        if (statut == StatutCycle.FACTURE_RECU || statut == StatutCycle.CLOTURE) {
            throw new IllegalArgumentException(
                    "Étape 2 verrouillée : la facture régulière du dossier " + cycle.getNumeroReference() + " existe déjà");
        }
        validerLignes(dto);

        FactureRequestDTO factureDto = toFactureDto(dto, Facture.TypeFacture.BON_COMMANDE, cycle, null);
        FactureResponseDTO bc;
        if (cycle.getBonCommande() != null) {
            bc = factureService.update(cycle.getBonCommande().getId(), factureDto);   // régénération
        } else {
            bc = factureService.create(factureDto);
        }

        Facture bcEntity = factureRepository.findById(bc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande non trouvé"));
        cycle.setBonCommande(bcEntity);
        if (cycle.getDateBonCommande() == null) {
            cycle.setDateBonCommande(java.time.LocalDateTime.now());
        }
        cycle.setStatut(StatutCycle.BC_GENERE);

        FacturationCycle saved = cycleRepository.save(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Bon de commande " + bc.getNumeroFacture() + " généré pour le dossier " + saved.getNumeroReference(),
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        log.info("Étape 2 validée (BC {}) pour le dossier #{}", bc.getNumeroFacture(), saved.getId());
        return toDto(saved);
    }

    /** PDF OHADA du bon de commande (étape 2). */
    @Transactional(readOnly = true)
    public byte[] pdfBonCommande(Long id) {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getBonCommande() == null) {
            throw new IllegalArgumentException("Aucun bon de commande généré pour ce dossier");
        }
        return factureService.genererPdf(cycle.getBonCommande().getId());
    }

    // ─── Étape 3 — FACTURE RÉGULIÈRE (après paiement) ─────────────────────────

    /**
     * Étape 3 (saisie) : crée (ou régénère) la facture régulière chaînée au BC.
     * Seule la régulière VALIDEE est comptabilisée (écritures OHADA).
     */
    @Transactional
    public FacturationCycleResponseDTO creerFactureFinale(Long id, FacturationDocumentRequestDTO dto) {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getBonCommande() == null) {
            throw new IllegalArgumentException(
                    "Étape 2 obligatoire : générez d'abord le bon de commande du dossier " + cycle.getNumeroReference());
        }
        if (cycle.isCloture()) {
            throw new IllegalArgumentException("Dossier clôturé : plus aucune modification possible");
        }
        validerLignes(dto);

        FactureRequestDTO factureDto = toFactureDto(dto, Facture.TypeFacture.REGULIERE,
                cycle, cycle.getBonCommande().getId());
        FactureResponseDTO fin;
        if (cycle.getFactureFinale() != null) {
            fin = factureService.update(cycle.getFactureFinale().getId(), factureDto);   // correction
        } else {
            fin = factureService.create(factureDto);
        }

        Facture finEntity = factureRepository.findById(fin.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Facture régulière non trouvée"));
        cycle.setFactureFinale(finEntity);
        if (cycle.getDateFactureFinale() == null) {
            cycle.setDateFactureFinale(java.time.LocalDateTime.now());
        }
        cycle.setStatut(StatutCycle.FACTURE_RECU);

        FacturationCycle saved = cycleRepository.save(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Facture régulière " + fin.getNumeroFacture() + " saisie pour le dossier " + saved.getNumeroReference(),
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        log.info("Étape 3 validée (régulière {}) pour le dossier #{}", fin.getNumeroFacture(), saved.getId());
        return toDto(saved);
    }

    /**
     * Étape 3 (alternative) : réception d'un PDF de facture finale (pièce
     * externe) au lieu de la saisie — le cycle peut alors être clôturé.
     */
    @Transactional
    public FacturationCycleResponseDTO uploadFactureFinalePdf(Long id, MultipartFile fichier) throws IOException {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getBonCommande() == null) {
            throw new IllegalArgumentException(
                    "Étape 2 obligatoire : générez d'abord le bon de commande du dossier " + cycle.getNumeroReference());
        }
        if (cycle.isCloture()) {
            throw new IllegalArgumentException("Dossier clôturé : plus aucune modification possible");
        }
        DocumentResponseDTO doc = documentService.uploadCyclePdf(
                "Facture finale — " + cycle.getNumeroReference(),
                "Étape 3/3 : facture régulière reçue (dossier " + cycle.getNumeroReference() + ")",
                fichier);

        Document piece = documentRepository.findById(doc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pièce facture finale non trouvée"));
        cycle.setFactureFinaleDocument(piece);
        if (cycle.getDateFactureFinale() == null) {
            cycle.setDateFactureFinale(java.time.LocalDateTime.now());
        }
        cycle.setStatut(StatutCycle.FACTURE_RECU);

        FacturationCycle saved = cycleRepository.save(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Facture finale reçue pour le dossier " + saved.getNumeroReference() + " (étape 3/3 validée)",
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        return toDto(saved);
    }

    /** Encaissement de la facture régulière (délègue FactureService — trésorerie OHADA). */
    @Transactional
    public FactureResponseDTO enregistrerPaiement(Long id, PaiementFactureRequestDTO dto) {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getFactureFinale() == null) {
            throw new IllegalArgumentException("Aucune facture régulière à encaisser pour ce dossier");
        }
        return factureService.enregistrerPaiement(cycle.getFactureFinale().getId(), dto);
    }

    /** Historique des règlements de la facture régulière. */
    @Transactional(readOnly = true)
    public List<PaiementFactureResponseDTO> getPaiements(Long id) {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.getFactureFinale() == null) {
            return List.of();
        }
        return factureService.getPaiements(cycle.getFactureFinale().getId());
    }

    // ─── Clôture / suppression ────────────────────────────────────────────────

    /**
     * Clôture du cycle — les 3 étapes sont obligatoires ; si une facture
     * régulière a été saisie, elle doit être intégralement réglée avant clôture.
     */
    @Transactional
    public FacturationCycleResponseDTO cloturer(Long id) {
        FacturationCycle cycle = requireCycle(id);
        if (!cycle.hasProforma() || !cycle.hasBonCommande() || !cycle.hasFactureFinale()) {
            throw new IllegalArgumentException(
                    "Clôture impossible : les 3 étapes du cycle doivent être complétées (proforma, bon de commande, facture régulière)");
        }
        if (cycle.getFactureFinale() != null) {
            BigDecimal restant = cycle.getFactureFinale().getMontantRestant();
            if (restant != null && restant.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException(
                        "Clôture impossible : encours restant de " + restant.toPlainString()
                                + " FCFA — encaissez le solde avant de clôturer");
            }
        }
        cycle.setStatut(StatutCycle.CLOTURE);
        cycle.setDateCloture(java.time.LocalDateTime.now());

        FacturationCycle saved = cycleRepository.save(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.TOGGLE_STATUS,
                "Dossier de facturation " + saved.getNumeroReference() + " clôturé (cycle complet)",
                "FacturationCycle", saved.getId(), saved.getNumeroReference());
        log.info("Dossier de facturation #{} clôturé", saved.getId());
        return toDto(saved);
    }

    /** Suppression — refusée dès qu'un document comptable est rattaché. */
    @Transactional
    public void delete(Long id) {
        FacturationCycle cycle = requireCycle(id);
        if (cycle.hasBonCommande() || cycle.hasFactureFinale()) {
            throw new IllegalArgumentException(
                    "Suppression impossible : des documents comptables sont rattachés au dossier " + cycle.getNumeroReference());
        }
        cycleRepository.delete(cycle);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Suppression du dossier de facturation " + cycle.getNumeroReference(),
                "FacturationCycle", id, cycle.getNumeroReference());
        log.info("Dossier de facturation #{} supprimé", id);
    }

    // ─── Utilitaires privés ───────────────────────────────────────────────────

    /** Charge le dossier et vérifie l'isolation multi-tenant. */
    private FacturationCycle requireCycle(Long id) {
        FacturationCycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier de facturation non trouvé : " + id));
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        if (scope != null && cycle.getEntreprise() != null && !scope.equals(cycle.getEntreprise().getId())) {
            throw new ResourceNotFoundException("Dossier de facturation non trouvé : " + id);
        }
        return cycle;
    }

    private void validerLignes(FacturationDocumentRequestDTO dto) {
        if (dto.getLignesFacture() == null || dto.getLignesFacture().isEmpty()) {
            throw new IllegalArgumentException("Au moins une ligne de saisie est obligatoire");
        }
    }

    /** Conversion des champs de saisie du cycle vers le DTO facture générique (OHADA). */
    private FactureRequestDTO toFactureDto(FacturationDocumentRequestDTO dto, Facture.TypeFacture type,
                                           FacturationCycle cycle, Long origineId) {
        if (cycle.getProjet() == null) {
            throw new IllegalArgumentException("Le dossier n'a pas de projet rattaché");
        }
        FactureRequestDTO f = new FactureRequestDTO();
        f.setTitre(dto.getTitre());
        f.setDescription(dto.getDescription());
        f.setDateEmission(dto.getDateEmission());
        f.setModule(dto.getModule());
        f.setTauxTVA(dto.getTauxTVA() != null ? dto.getTauxTVA() : new BigDecimal("19.25"));
        // OHADA contextualisé : facturation interne (client du groupe) => pas de TVA.
        f.setAssujettiTVA(dto.getAssujettiTVA() != null ? dto.getAssujettiTVA() : cycle.getClient() == null);
        f.setTypeFacture(type);
        f.setProjetId(cycle.getProjet().getId());
        f.setClientId(cycle.getClient() != null ? cycle.getClient().getId() : null);
        f.setSens(cycle.getSens());
        f.setNomFournisseur(cycle.getSens() == Facture.SensFacture.ACHAT ? cycle.getNomFournisseur() : null);
        f.setFactureOrigineId(origineId);
        f.setLignesFacture(dto.getLignesFacture());
        f.setMontantHT(dto.getMontantHT());
        return f;
    }

    private String referenceProvisoire() {
        return "CYC-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** Mapping entité → DTO avec états d'avancement des 3 étapes (barre de progression). */
    private FacturationCycleResponseDTO toDto(FacturationCycle c) {
        FacturationCycleResponseDTO dto = new FacturationCycleResponseDTO();
        dto.setId(c.getId());
        dto.setNumeroReference(c.getNumeroReference());
        dto.setStatut(c.getStatut());
        dto.setTitre(c.getTitre());
        dto.setDescription(c.getDescription());
        dto.setSens(c.getSens());
        dto.setNomFournisseur(c.getNomFournisseur());
        dto.setCommentaire(c.getCommentaire());

        dto.setClientId(c.getClient() != null ? c.getClient().getId() : null);
        dto.setClientNom(c.getClient() != null ? c.getClient().getName() : null);
        dto.setProjetId(c.getProjet() != null ? c.getProjet().getId() : null);
        dto.setProjetNom(c.getProjet() != null ? c.getProjet().getNom() : null);
        dto.setProjetCode(c.getProjet() != null ? c.getProjet().getCodeProjet() : null);

        boolean e1 = c.hasProforma();
        boolean e2 = c.hasBonCommande();
        boolean e3 = c.hasFactureFinale();
        dto.setEtape1Faite(e1);
        dto.setEtape2Faite(e2);
        dto.setEtape3Faite(e3);
        dto.setEtapeCourante(c.etapeCourante());
        dto.setAvancementPct(c.isCloture() ? 100 : e3 ? 67 : e2 ? 34 : 0);
        dto.setCloturee(c.isCloture());

        // Étape 1 — pièce proforma
        if (c.getProformaDocument() != null) {
            FacturationCycleResponseDTO.PieceProforma p = new FacturationCycleResponseDTO.PieceProforma();
            p.setDocumentId(c.getProformaDocument().getId());
            p.setNom(c.getProformaDocument().getNom());
            p.setTaille(c.getProformaDocument().getTaille());
            p.setDate(c.getDateProforma());
            dto.setProforma(p);
        }
        // Étape 3 alternative — pièce facture finale
        if (c.getFactureFinaleDocument() != null) {
            FacturationCycleResponseDTO.PieceProforma p = new FacturationCycleResponseDTO.PieceProforma();
            p.setDocumentId(c.getFactureFinaleDocument().getId());
            p.setNom(c.getFactureFinaleDocument().getNom());
            p.setTaille(c.getFactureFinaleDocument().getTaille());
            p.setDate(c.getDateFactureFinale());
            dto.setPieceFinale(p);
        }
        // Étapes 2 / 3 — résumés des factures
        dto.setBonCommande(resume(c.getBonCommande()));
        dto.setFactureFinale(resume(c.getFactureFinale()));

        // Totaux : issue de la régulière, sinon du BC
        Facture totaux = c.getFactureFinale() != null ? c.getFactureFinale() : c.getBonCommande();
        dto.setMontantTTC(totaux != null ? totaux.getMontantTTC() : null);
        dto.setMontantPaye(totaux != null ? totaux.getMontantPaye() : null);
        dto.setMontantRestant(totaux != null ? totaux.getMontantRestant() : null);

        dto.setDateCreation(c.getDateCreation());
        dto.setDateProforma(c.getDateProforma());
        dto.setDateBonCommande(c.getDateBonCommande());
        dto.setDateFactureFinale(c.getDateFactureFinale());
        dto.setDateCloture(c.getDateCloture());
        return dto;
    }

    private FacturationCycleResponseDTO.DocResume resume(Facture f) {
        if (f == null) return null;
        FacturationCycleResponseDTO.DocResume r = new FacturationCycleResponseDTO.DocResume();
        r.setId(f.getId());
        r.setNumero(f.getNumeroFacture());
        r.setTitre(f.getTitre());
        r.setTypeFacture(f.getTypeFacture());
        r.setStatut(f.getStatut());
        r.setDateEmission(f.getDateEmission());
        r.setMontantHT(f.getMontantHT());
        r.setMontantTVA(f.getMontantTVA());
        r.setMontantTTC(f.getMontantTTC());
        r.setMontantPaye(f.getMontantPaye());
        r.setMontantRestant(f.getMontantRestant());
        r.setPdfDisponible(true);
        return r;
    }
}