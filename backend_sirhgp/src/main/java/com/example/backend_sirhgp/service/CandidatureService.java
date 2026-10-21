// ─── CandidatureService.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.request.CandidatureDecisionRequest;
import com.example.backend_sirhgp.dto.request.CandidatureRequestDTO;
import com.example.backend_sirhgp.dto.request.EmbaucheRequestDTO;
import com.example.backend_sirhgp.dto.response.CandidatureResponseDTO;
import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.RecrutementMappers;
import com.example.backend_sirhgp.model.Candidature;
import com.example.backend_sirhgp.model.Departement;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.OffreEmploi;
import com.example.backend_sirhgp.model.Poste;
import com.example.backend_sirhgp.model.Role;
import com.example.backend_sirhgp.repository.CandidatureRepository;
import com.example.backend_sirhgp.repository.DepartementRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.OffreEmploiRepository;
import com.example.backend_sirhgp.repository.PosteRepository;
import com.example.backend_sirhgp.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final OffreEmploiRepository offreRepository;
    private final EmployerRepository employerRepository;
    private final PosteRepository posteRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecrutementMappers mappers;
    private final AuditLogService auditLogService;
    private final EntrepriseContextService entrepriseContextService;
    private final DepartementRepository departementRepository;
    private final DocumentService documentService;
    // Refonte LEAD 6.0 : périmètre départemental du recruteur + rattachement de
    // l'employé embauché (succursale d'origine, supérieur hiérarchique).
    private final LeadPerimetreService leadPerimetre;
    private final SucursalAttributionService sucursalAttribution;

    /** Transitions autorisées du pipeline de recrutement (5.13 : pipeline simplifié à 4 étapes).
     * Les anciens statuts (PRESELECTION, TEST, OFFRE) ne sont plus des cibles : seules leurs
     * sorties manuelles vers le pipeline simplifié — ou vers REFUSEE — restent autorisées,
     * le temps que ces dossiers soient déplacés (la migration SQL refuse de restreindre
     * l'ENUM tant qu'il en reste). */
    private static final Map<Candidature.StatutCandidature, Set<Candidature.StatutCandidature>> TRANSITIONS = Map.of(
            Candidature.StatutCandidature.NOUVELLE,
            EnumSet.of(Candidature.StatutCandidature.ENTRETIEN, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.ENTRETIEN,
            EnumSet.of(Candidature.StatutCandidature.EVALUATION, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.EVALUATION,
            EnumSet.of(Candidature.StatutCandidature.RECRUTE, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.PRESELECTION,
            EnumSet.of(Candidature.StatutCandidature.ENTRETIEN, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.TEST,
            EnumSet.of(Candidature.StatutCandidature.EVALUATION, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.OFFRE,
            EnumSet.of(Candidature.StatutCandidature.EVALUATION, Candidature.StatutCandidature.REFUSEE),
            Candidature.StatutCandidature.RECRUTE, EnumSet.noneOf(Candidature.StatutCandidature.class),
            Candidature.StatutCandidature.REFUSEE, EnumSet.noneOf(Candidature.StatutCandidature.class)
    );

    @Transactional
    public CandidatureResponseDTO create(CandidatureRequestDTO dto) {
        return create(dto, null);
    }

    /**
     * Variante interne (refonte 5.7) : création d'une candidature dont le CV a déjà
     * été téléversé — le documentId est rattaché dans la même transaction afin que
     * la candidature et son CV soient persistés ensemble.
     */
    private CandidatureResponseDTO create(CandidatureRequestDTO dto, Long documentId) {
        OffreEmploi offre = offreRepository.findById(dto.getOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée : " + dto.getOffreId()));
        if (offre.getStatut() != OffreEmploi.StatutOffre.OUVERTE) {
            throw new IllegalArgumentException("Cette offre n'est plus ouverte aux candidatures");
        }
        if (candidatureRepository.existsByEmailIgnoreCaseAndOffreId(dto.getEmail().trim(), offre.getId())) {
            throw new IllegalArgumentException("Une candidature avec cet email existe déjà pour cette offre");
        }

        Entreprise entreprise = offre.getEntreprise();

        Candidature candidature = Candidature.builder()
                .offre(offre)
                .nomCandidat(dto.getNomCandidat().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .telephone(dto.getTelephone())
                .cvUrl(dto.getCvUrl())
                .lettreMotivation(dto.getLettreMotivation())
                .commentaire(dto.getCommentaire())
                .dateCandidature(LocalDateTime.now())
                .statut(Candidature.StatutCandidature.NOUVELLE)
                .entreprise(entreprise)
                .documentId(documentId)
                .build();

        if (dto.getEmployeId() != null) {
            candidature.setEmploye(employerRepository.findById(dto.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + dto.getEmployeId())));
        }

        Candidature saved = candidatureRepository.save(candidature);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Nouvelle candidature de '" + saved.getNomCandidat() + "' sur l'offre '" + offre.getTitre() + "'",
                "Candidature", saved.getId(), saved.getNomCandidat());
        log.info("Candidature créée : {} sur {}", saved.getNomCandidat(), offre.getTitre());
        return mappers.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> findAll() {
        Long scope = entrepriseContextService.getScopeEntrepriseId();
        List<Candidature> list = scope != null
                ? candidatureRepository.findByEntrepriseIdWithOffre(scope)
                : candidatureRepository.findAllWithOffre();
        // Refonte 5.7 : LEAD = uniquement les candidatures des offres de ses départements dirigés
        if (entrepriseContextService.isLead()) {
            Employer current = entrepriseContextService.getCurrentEmployer();
            var idsDiriges = current == null ? java.util.Set.<Long>of() :
                    departementRepository.findBySuperviseurId(current.getId()).stream()
                            .map(Departement::getId).collect(java.util.stream.Collectors.toSet());
            list = list.stream()
                    .filter(c -> c.getOffre() != null && c.getOffre().getDepartement() != null
                            && idsDiriges.contains(c.getOffre().getDepartement().getId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        // 5.9 : scoping fin par sucursale de l'offre (MANAGER/LEAD rattachés) — null-inclusif.
        Long scopeSucursalId = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursalId != null) {
            final Long sc = scopeSucursalId;
            list = list.stream()
                    .filter(c -> c.getOffre() == null || c.getOffre().getSucursal() == null
                            || sc.equals(c.getOffre().getSucursal().getId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return list.stream().map(mappers::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * Vérifie que l'utilisateur courant peut agir sur l'offre (scope LEAD 5.7).
     * LEAD : offre rattachée à l'un de ses départements dirigés, sinon 403.
     * MANAGER / ADMIN : tout le scope entreprise (aucun filtre départemental).
     */
    private void verifierScopeOffre(OffreEmploi offre) {
        if (!entrepriseContextService.isLead()) return;
        Employer current = entrepriseContextService.getCurrentEmployer();
        Long deptId = offre.getDepartement() != null ? offre.getDepartement().getId() : null;
        boolean autorise = deptId != null && departementRepository.findBySuperviseurId(current.getId()).stream()
                .anyMatch(d -> d.getId().equals(deptId));
        if (!autorise) {
            throw new AccessDeniedException(
                    "Cette offre appartient au département '"
                            + (offre.getDepartement() != null ? offre.getDepartement().getName() : "sans département")
                            + "' — hors de votre périmètre");
        }
    }

    /** Vérifie le scope via la candidature (offre rattachée). */
    private void verifierScopeCandidature(Candidature candidature) {
        if (candidature.getOffre() == null) {
            throw new IllegalArgumentException("Candidature orpheline : aucune offre rattachée");
        }
        verifierScopeOffre(candidature.getOffre());
    }

    // ─── Refonte 5.7 : création avec CV téléversé ─────────────────────────
    // Upload le CV (Document typé CV, sans destinataire — le candidat est externe)
    // puis crée la candidature rattachée à l'offre avec documentId renseigné.
    // En cas d'échec de la création, le document orphelin est marqué SUPPRIME.
    @Transactional
    public CandidatureResponseDTO createAvecCv(CandidatureRequestDTO dto, MultipartFile cv) throws java.io.IOException {
        if (cv == null || cv.isEmpty()) {
            throw new IllegalArgumentException("Le CV est obligatoire");
        }
        DocumentResponseDTO doc = documentService.uploadCvCandidat(
                "CV_" + dto.getNomCandidat().trim().replaceAll("\\s+", "_"), cv);
        try {
            return create(dto, doc.getId());
        } catch (RuntimeException e) {
            try { documentService.deleteDocument(doc.getId()); } catch (Exception ignored) { /* déjà loggé */ }
            throw e;
        }
    }

    /** Ajoute (ou remplace) le CV d'une candidature existante (scope LEAD vérifié). */
    @Transactional
    public CandidatureResponseDTO uploadCv(Long id, MultipartFile cv) throws java.io.IOException {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée : " + id));
        verifierScopeCandidature(candidature);
        if (cv == null || cv.isEmpty()) {
            throw new IllegalArgumentException("Le CV est obligatoire");
        }
        DocumentResponseDTO doc = documentService.uploadCvCandidat(
                "CV_" + candidature.getNomCandidat().trim().replaceAll("\\s+", "_"), cv);
        candidature.setDocumentId(doc.getId());
        Candidature saved = candidatureRepository.save(candidature);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "CV ajouté à la candidature de '" + saved.getNomCandidat() + "'",
                "Candidature", saved.getId(), saved.getNomCandidat());
        return mappers.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponseDTO> findByOffreId(Long offreId) {
        OffreEmploi offre = offreRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée : " + offreId));
        // Refonte 5.7 : un LEAD ne liste que les candidatures des offres de son périmètre
        verifierScopeOffre(offre);
        return candidatureRepository.findByOffreIdWithOffre(offreId).stream()
                .map(mappers::toResponseDTO).collect(Collectors.toList());
    }

    /** Avancement dans le pipeline avec validation de la transition. */
    @Transactional
    public CandidatureResponseDTO updateStatut(Long id, Candidature.StatutCandidature statut, String commentaire) {
        Candidature candidature = candidatureRepository.findByIdWithOffre(id);
        if (candidature == null) {
            throw new ResourceNotFoundException("Candidature non trouvée : " + id);
        }
        // Refonte 5.7 : scope LEAD sur l'avancement pipeline
        verifierScopeCandidature(candidature);

        Set<Candidature.StatutCandidature> autorises =
                TRANSITIONS.getOrDefault(candidature.getStatut(), EnumSet.noneOf(Candidature.StatutCandidature.class));
        if (!autorises.contains(statut)) {
            throw new IllegalArgumentException("Transition non autorisée : " + candidature.getStatut() + " → " + statut);
        }

        candidature.setStatut(statut);
        if (commentaire != null && !commentaire.isBlank()) {
            candidature.setCommentaire(commentaire);
        }

        // 5.9 — Automatisation : tout passage en RECRUTE rattache (ou crée) le compte
        // employé — la candidature RECRUTE est TOUJOURS un compte dans la liste Employés.
        // Idempotent : si un employé est déjà rattaché (candidat interne, embauche
        // antérieure), aucune création. Échec (email déjà pris, entreprise
        // indéterminable) → rollback : la transition n'a pas lieu (400 explicite).
        if (statut == Candidature.StatutCandidature.RECRUTE && candidature.getEmploye() == null) {
            Employer employer = creerEmployeDepuisCandidature(candidature, new EmbaucheRequestDTO());
            return finaliserEmbauche(candidature, employer);
        }

        Candidature saved = candidatureRepository.save(candidature);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Candidature de '" + saved.getNomCandidat() + "' : " + statut,
                "Candidature", saved.getId(), saved.getNomCandidat());
        return mappers.toResponseDTO(saved);
    }

    /** Pré-évaluation du dossier candidat : verdict + note + avis (4.3). */
    @Transactional
    public CandidatureResponseDTO evaluer(Long id, CandidatureDecisionRequest dto) {
        Candidature candidature = candidatureRepository.findByIdWithOffre(id);
        if (candidature == null) {
            throw new ResourceNotFoundException("Candidature non trouvée : " + id);
        }
        // Refonte 5.7 : scope LEAD sur l'évaluation
        verifierScopeCandidature(candidature);
        if (candidature.getStatut() == Candidature.StatutCandidature.RECRUTE
                || candidature.getStatut() == Candidature.StatutCandidature.REFUSEE) {
            throw new IllegalArgumentException("Candidature déjà terminée (RECRUTE/REFUSEE) — impossible de ré-évaluer");
        }

        if (dto.getDecision() != null) candidature.setDecision(dto.getDecision());
        else candidature.setDecision(Candidature.DecisionRecrutement.A_EVALUER);
        if (dto.getNote() != null) candidature.setNoteEvaluation(dto.getNote());
        if (dto.getAvis() != null) candidature.setAvisEvaluation(dto.getAvis());
        if (dto.getDocumentId() != null) candidature.setDocumentId(dto.getDocumentId());

        Candidature saved = candidatureRepository.save(candidature);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Pré-évaluation de '" + saved.getNomCandidat() + "' : " + saved.getDecision()
                        + (saved.getNoteEvaluation() != null ? " (" + saved.getNoteEvaluation() + "/20)" : ""),
                "Candidature", saved.getId(), saved.getNomCandidat());
        log.info("Pré-évaluation candidature {} : {}", id, saved.getDecision());
        return mappers.toResponseDTO(saved);
    }

    /** Embauche d'un candidat externe : crée l'employé + bascule la candidature en RECRUTE/ENGAGE (4.3). */
    @Transactional
    public CandidatureResponseDTO embaucher(Long id, EmbaucheRequestDTO dto) {
        Candidature candidature = candidatureRepository.findByIdWithOffre(id);
        if (candidature == null) {
            throw new ResourceNotFoundException("Candidature non trouvée : " + id);
        }
        if (candidature.getStatut() == Candidature.StatutCandidature.RECRUTE) {
            throw new IllegalArgumentException("Cette candidature a déjà été embauchée");
        }
        // Refonte 5.7 : l'embauche suit le même scope que l'évaluation (LEAD = ses départements)
        verifierScopeCandidature(candidature);

        Employer employer = creerEmployeDepuisCandidature(candidature, dto);
        return finaliserEmbauche(candidature, employer);
    }

    /**
     * Crée le compte employé à partir de l'identité du candidat — 5.9 : SOURCE UNIQUE
     * de création, partagée par l'embauche manuelle (modal « Nouvel employé ») et par
     * la transition automatique EVALUATION → RECRUTE. Idempotent : renvoie l'employé déjà
     * rattaché à la candidature au lieu d'en créer un second.
     */
    private Employer creerEmployeDepuisCandidature(Candidature candidature, EmbaucheRequestDTO dto) {
        if (candidature.getEmploye() != null) {
            return candidature.getEmploye();
        }

        // L'email ne doit pas être déjà pris par un employé
        String email = candidature.getEmail().trim().toLowerCase();
        if (employerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Un employé existe déjà avec cet email (" + email + "). Embauche impossible en l'état.");
        }

        OffreEmploi offre = candidature.getOffre();
        Entreprise entreprise = candidature.getEntreprise();
        if (entreprise == null && offre != null) entreprise = offre.getEntreprise();
        if (entreprise == null) {
            throw new IllegalArgumentException("Impossible de déterminer l'entreprise d'accueil");
        }

        // Poste : préférer celui saisi sinon celui de l'offre
        Poste poste = null;
        if (dto.getPosteId() != null) {
            poste = posteRepository.findById(dto.getPosteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste non trouvé : " + dto.getPosteId()));
        } else if (offre != null) {
            poste = offre.getPoste();
        }

        // Refonte LEAD 6.0 : le chef de département recrute POUR SON ÉQUIPE.
        // Le compte créé doit être rattaché à l'un de ses départements, sinon il
        // tomberait hors de sa liste de employés (filtre findByDepartementIdIn)
        // et il ne pourrait même pas voir le salarié qu'il vient d'embaucher.
        if (leadPerimetre.estLead()) {
            Long departementCible = poste != null && poste.getDepartement() != null
                    ? poste.getDepartement().getId() : null;
            if (departementCible == null) {
                throw new IllegalArgumentException(
                        "Sélectionnez un poste rattaché à un de vos départements : "
                                + "le compte embauché doit rejoindre votre équipe.");
            }
            leadPerimetre.exigerDepartementSupervise(departementCible, "Embauche d'un candidat");

            // Cohérence : le departementId saisi (lorsqu'il est fourni) doit être
            // celui du poste — ce champ était jusqu'ici lu par PERSONNE.
            if (dto.getDepartementId() != null && !dto.getDepartementId().equals(departementCible)) {
                throw new IllegalArgumentException(
                        "Le département indiqué ne correspond pas au poste sélectionné");
            }
        } else if (dto.getDepartementId() != null && poste != null && poste.getDepartement() != null
                && !dto.getDepartementId().equals(poste.getDepartement().getId())) {
            throw new IllegalArgumentException(
                    "Le département indiqué ne correspond pas au poste sélectionné");
        }

        // Garde-fou légal : salaire ≥ SMIG camerounais (5.1) — seulement si un salaire est saisi
        if (dto.getSalaireBase() != null
                && dto.getSalaireBase().compareTo(com.example.backend_sirhgp.service.PaieService.SMIG_MENSUEL) < 0) {
            throw new IllegalArgumentException(
                    "Salaire brut inférieur au SMIG camerounais (42 690 FCFA) — embauche refusée.");
        }

        // Rôle par défaut. Refonte LEAD 6.0 : un chef de département ne peut pas
        // créer un compte de niveau MANAGER ou ADMIN (élévation de privilège :
        // il s'attribuerait un subordonné capable de le valider/remplacer).
        String roleDemandee = dto.getRoleName() != null ? dto.getRoleName() : "STAFF";
        if (leadPerimetre.estLead()
                && !("STAFF".equalsIgnoreCase(roleDemandee) || "STAGIAIRE".equalsIgnoreCase(roleDemandee))) {
            throw new IllegalArgumentException(
                    "En tant que chef de département, vous ne pouvez recruter qu'un profil "
                            + "STAFF ou STAGIAIRE (rôle demandé : " + roleDemandee + ").");
        }
        Role role = roleRepository.findByName(roleDemandee)
                .orElse(null);

        Employer employer = Employer.builder()
                .name(candidature.getNomCandidat().trim())
                .email(email)
                .numeroTelephone(candidature.getTelephone())
                // 5.12 — CV reconduit de la candidature : le document joint devient le CV de
                // la fiche employé (même convention d'URL que la page Employés). À défaut de
                // document, on conserve l'éventuel lien historique (candidatures legacy).
                .cvUrl(candidature.getDocumentId() != null
                        ? "/api/documents/" + candidature.getDocumentId() + "/download"
                        : candidature.getCvUrl())
                .poste(poste)
                .role(role)
                .entreprise(entreprise)
                .motDePasse(passwordEncoder.encode(randomPassword(12)))
                .nui(dto.getNui())
                .salaireBase(dto.getSalaireBase())
                .dateEmbauche(dto.getDateEmbauche() != null ? dto.getDateEmbauche() : LocalDate.now())
                .actif(true)
                // Refonte LEAD 6.0 — les trois rattachements manquants. Sans eux,
                // l'employé embauché restait « orphelin » de structure :
                //  · sans succursale → invisible de toute liste scopée succursale
                //    (findBySucursalId), donc absent pour le MANAGER comme pour le LEAD ;
                //  · sans manager   → absent de l'organigramme, personne ne le supervise
                //    dans la hiérarchie ;
                //  · sans département → impossible à rattacher à une équipe.
                // Le département est porté par le POSTE (Employer n'a pas de
                // departement_id) : le poste validé ci-dessus fait foi.
                .sucursal(sucursalAttribution.resoudreCreation(null, entreprise))
                .manager(recruteurSiLead())
                .build();
        Employer saved = employerRepository.save(employer);

        // 5.12 — le document CV est rattaché au compte créé (fiche employé + documents
        // reçus) : aucun second téléversement n'est demandé à l'utilisateur.
        // (sans CV au dossier, aucune reconduction n'est tentée)
        if (candidature.getDocumentId() != null) {
            documentService.rattacherCvAuNouvelEmploye(candidature.getDocumentId(), saved);
        }
        return saved;
    }

    /**
     * Superviseur du recruté : le chef de département qui recrute, si l'appelant
     * est un LEAD. Pour l'ADMIN / MANAGER, aucun rattachement automatique (le
     * supérieur est choisi dans la fiche après coup) — on ne veut pas imposer
     * au recruteur lui-même comme chef de son nouveau subordonné.
     */
    private Employer recruteurSiLead() {
        return leadPerimetre.estLead()
                ? entrepriseContextService.getCurrentEmployer()
                : null;
    }

    /** Bascule la candidature en RECRUTE/ENGAGE, rattache l'employé et journalise (4.3 + 5.9). */
    private CandidatureResponseDTO finaliserEmbauche(Candidature candidature, Employer employer) {
        candidature.setEmploye(employer);
        candidature.setDateEmbauche(employer.getDateEmbauche());
        candidature.setStatut(Candidature.StatutCandidature.RECRUTE);
        candidature.setDecision(Candidature.DecisionRecrutement.ENGAGE);
        candidature.setCommentaire("Embauché(e) — " + (employer.getPoste() != null ? employer.getPoste().getName() : "sans poste"));
        Candidature saved = candidatureRepository.save(candidature);

        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Embauche de '" + employer.getName() + "' (candidature #" + saved.getId() + ") → employé #" + employer.getId(),
                "Employer", employer.getId(), employer.getName());
        log.info("Candidature {} embauchée → employé #{}", saved.getId(), employer.getId());
        return mappers.toResponseDTO(saved);
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private String randomPassword(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    @Transactional
    public void delete(Long id) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature non trouvée : " + id));
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Suppression de la candidature de '" + candidature.getNomCandidat() + "'",
                "Candidature", candidature.getId(), candidature.getNomCandidat());
        candidatureRepository.delete(candidature);
    }
}