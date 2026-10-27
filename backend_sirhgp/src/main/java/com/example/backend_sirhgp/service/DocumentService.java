package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.DocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.DocumentTransferRequestDTO;
import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.event.DocumentDispatchedEvent;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.exception.UnauthorizedException;
import com.example.backend_sirhgp.mapper.DocumentMapper;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Document.DocumentStatus;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.repository.DocumentRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployerRepository employerRepository;
    private final ProjetRepository projetRepository;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionApproverResolver permissionApproverResolver;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public DocumentResponseDTO uploadCyclePdf(String nom, String description, MultipartFile fichier) throws IOException {
        return uploadStandalone(nom, description, Document.TypeDocument.FACTURE, fichier);
    }

    @Transactional
    public DocumentResponseDTO uploadCvCandidat(String nom, MultipartFile fichier) throws IOException {
        return uploadStandalone(nom, "CV de candidature", Document.TypeDocument.AUTRE, fichier);
    }

    @Transactional
    public DocumentResponseDTO uploadMyCv(String nom, MultipartFile fichier) throws IOException {
        return uploadStandalone(nom, "CV personnel", Document.TypeDocument.CV, fichier);
    }

    private DocumentResponseDTO uploadStandalone(String nom, String description, Document.TypeDocument type,
                                                 MultipartFile fichier) throws IOException {
        validateFile(fichier);
        Path path = savePhysicalFile(fichier);
        Employer expediteur = getCurrentUser();
        Document document = Document.builder()
                .nom(nom)
                .description(description)
                .nomFichier(path.getFileName().toString())
                .typeMime(fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream")
                .cheminStockage(path.toString())
                .taille(fichier.getSize())
                .type(type)
                .status(DocumentStatus.ACTIF)
                .publique(false)
                .expediteur(expediteur)
                .employer(expediteur)
                .build();
        return documentMapper.toResponseDTO(documentRepository.save(document));
    }

    @Transactional
    public void rattacherCvAuNouvelEmploye(Long documentId, Employer nouvelEmploye) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("CV non trouvé : " + documentId));
        document.setEmployer(nouvelEmploye);
        documentRepository.save(document);
    }

    // ─── Upload + dispatch OBLIGATOIRE ────────────────────────────────────────
    // Crée UN SEUL document avec expediteur + destinataire. Pas de clonage.
    // Le super admin ne peut pas être destinataire mais peut envoyer.
    @Transactional
    public DocumentResponseDTO uploadWithDispatch(DocumentRequestDTO dto,
                                                  MultipartFile fichier) throws IOException {
        Employer expediteur = getCurrentUser();

        if (dto.getDestinataireId() == null) {
            throw new IllegalArgumentException("Un destinataire est obligatoire lors de l'upload");
        }

        validateFile(fichier);

        Employer destinataire = employerRepository.findById(dto.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé: " + dto.getDestinataireId()));

        if (isSuperAdmin(destinataire)) {
            throw new IllegalArgumentException("Impossible d'envoyer un document au Super Administrateur");
        }
        DocumentScope scope = buildScope(expediteur);
        verifierEmployeDansScope(scope, destinataire);

        Path filePath = savePhysicalFile(fichier);

        Document document = Document.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .type(dto.getType())
                .publique(false)
                .nomFichier(filePath.getFileName().toString())
                .cheminStockage(filePath.toAbsolutePath().toString())
                .typeMime(fichier.getContentType())
                .taille(fichier.getSize())
                .expediteur(expediteur)
                .destinataire(destinataire)
                .dateEnvoi(LocalDateTime.now())
                .nombreTelechargements(0L)
                .status(DocumentStatus.ACTIF)
                .build();

        if (dto.getEmployerId() != null) {
            Employer lie = employerRepository.findById(dto.getEmployerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + dto.getEmployerId()));
            verifierEmployeDansScope(scope, lie);
            document.setEmployer(lie);
        }
        if (dto.getProjetId() != null) {
            var projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            if (!isProjetDansScope(scope, projet))
                throw new org.springframework.security.access.AccessDeniedException("Projet hors de votre périmÃ¨tre.");
            document.setProjet(projet);
        }

        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentDispatchedEvent(
                this, saved, expediteur, destinataire, null));

        log.info("Document '{}' uploadé et dispatché par {} → {}",
                saved.getNom(), expediteur.getEmail(), destinataire.getEmail());

        return documentMapper.toResponseDTO(saved);
    }

    /** Upload a Timesheet permission attachment and route it to the role-chain approver. */
    @Transactional
    public DocumentResponseDTO uploadPermissionJustificatif(String nom, MultipartFile fichier) throws IOException {
        Employer expediteur = getCurrentUser();
        validateFile(fichier);

        Employer destinataire = permissionApproverResolver.resolve(expediteur);
        DocumentScope scope = buildScope(expediteur);
        if (destinataire != null) {
            if (isSuperAdmin(destinataire)) {
                throw new IllegalArgumentException("Impossible d'envoyer un justificatif au Super Administrateur");
            }
            verifierEmployeDansScope(scope, destinataire);
        }

        Path filePath = savePhysicalFile(fichier);
        Document document = Document.builder()
                .nom(nom == null || nom.isBlank() ? "Justificatif de permission" : nom.trim())
                .description("Justificatif joint à une demande de permission")
                .type(Document.TypeDocument.AUTRE)
                .publique(false)
                .nomFichier(filePath.getFileName().toString())
                .cheminStockage(filePath.toAbsolutePath().toString())
                .typeMime(fichier.getContentType())
                .taille(fichier.getSize())
                .expediteur(expediteur)
                .employer(expediteur)
                .destinataire(destinataire)
                .dateEnvoi(destinataire != null ? LocalDateTime.now() : null)
                .nombreTelechargements(0L)
                .status(DocumentStatus.ACTIF)
                .build();

        Document saved = documentRepository.save(document);
        if (destinataire != null) {
            eventPublisher.publishEvent(new DocumentDispatchedEvent(this, saved, expediteur, destinataire, null));
        }
        log.info("Justificatif de permission '{}' téléversé par {}{}", saved.getNom(), expediteur.getEmail(),
                destinataire != null ? " → " + destinataire.getEmail() : " (aucun valideur dédié)");
        return documentMapper.toResponseDTO(saved);
    }

    // ─── Redispatch d'un document existant (SANS clonage) ────────────────────
    // On met à jour le destinataire sur le même enregistrement.
    // L'expéditeur devient l'utilisateur qui redispatche, traçant la chaîne.
    @Transactional
    public DocumentResponseDTO transferDocument(Long documentId, DocumentTransferRequestDTO dto) {
        Employer expediteur = getCurrentUser();

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        DocumentScope scope = buildScope(expediteur);
        if (isSecretary(expediteur) && (document.getExpediteur() == null
                || !expediteur.getId().equals(document.getExpediteur().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Secretary can transfer only documents they sent.");
        }
        if (!hasAccess(scope, document)
                || (!document.appartientA(expediteur.getId()) && !isAdminOrManager(expediteur) && !isSuperAdmin(expediteur))) {
            throw new UnauthorizedException("User cannot transfer this document");
        }

        Employer nouveauDestinataire = employerRepository.findById(dto.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé: " + dto.getDestinataireId()));

        if (isSuperAdmin(nouveauDestinataire)) {
            throw new IllegalArgumentException("Impossible de transférer un document au Super Administrateur");
        }
        verifierEmployeDansScope(scope, nouveauDestinataire);

        // Mise à jour en place — zéro doublon
        document.setExpediteur(expediteur);
        document.setDestinataire(nouveauDestinataire);
        document.setDateEnvoi(LocalDateTime.now());
        document.setEstLu(false);
        document.setDateLecture(null);
        document.setStatus(DocumentStatus.ACTIF);

        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentDispatchedEvent(
                this, saved, expediteur, nouveauDestinataire, null));

        log.info("Document {} redispatché de {} à {}", documentId,
                expediteur.getEmail(), nouveauDestinataire.getEmail());

        return documentMapper.toResponseDTO(saved);
    }

    // ─── Métadonnées ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentMetadata(Long id) {
        Employer current = getCurrentUser();
        Document doc = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        if (!hasAccess(buildScope(current), doc))
            throw new ResourceNotFoundException("Document not found: " + id);
        return documentMapper.toResponseDTO(doc);
    }

    // ─── Documents reçus ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getReceivedDocuments(Long employerId) {
        Employer current = getCurrentUser();
        DocumentScope scope = buildScope(current);
        verifierEmployeAccessible(scope, employerId);
        return documentRepository.findReceivedDocuments(employerId, DocumentStatus.ACTIF).stream()
                .filter(doc -> hasAccess(scope, doc)).map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Documents non lus ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getUnreadDocuments(Long employerId) {
        Employer current = getCurrentUser();
        DocumentScope scope = buildScope(current);
        verifierEmployeAccessible(scope, employerId);
        return documentRepository.findUnreadReceivedDocuments(employerId, DocumentStatus.ACTIF).stream()
                .filter(doc -> hasAccess(scope, doc)).map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Documents envoyés ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getSentDocuments(Long employerId) {
        Employer current = getCurrentUser();
        DocumentScope scope = buildScope(current);
        verifierEmployeAccessible(scope, employerId);
        return documentRepository.findSentDocuments(employerId, DocumentStatus.ACTIF).stream()
                .filter(doc -> hasAccess(scope, doc)).map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Mes documents (expéditeur OU destinataire) ───────────────────────────
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getMyDocuments(Pageable pageable) {
        Employer currentUser = getCurrentUser();
        return listDocumentsForScope(currentUser, pageable);
    }

    // ─── Tous les documents (admins voient tout, autres voient les leurs) ─────
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getAllDocuments(Pageable pageable) {
        return listDocumentsForScope(getCurrentUser(), pageable);
    }

    // ─── Marquer comme lu ────────────────────────────────────────────────────
    @Transactional
    public void markAsRead(Long documentId) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        if (!hasAccess(buildScope(currentUser), document))
            throw new UnauthorizedException("You do not have access to this document");

        if (document.getDestinataire() == null ||
                !document.getDestinataire().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le destinataire de ce document");
        }
        document.marquerCommeLu();
        documentRepository.save(document);
    }

    // ─── Archiver ────────────────────────────────────────────────────────────
    @Transactional
    public void archiveDocument(Long documentId) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        if (isSecretary(currentUser) && (document.getExpediteur() == null
                || !currentUser.getId().equals(document.getExpediteur().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous pouvez archiver uniquement les documents que vous avez envoyés.");
        }

        if (!hasAccess(buildScope(currentUser), document)
                || (!document.appartientA(currentUser.getId()) && !isAdminOrManager(currentUser) && !isSuperAdmin(currentUser))) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à archiver ce document");
        }
        document.setEstArchive(true);
        documentRepository.save(document);
    }

    // ─── Télécharger ─────────────────────────────────────────────────────────
    // Accès : expéditeur, destinataire, admin, manager, super_admin
    @Transactional
    public byte[] downloadDocument(Long id) throws IOException {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        if (!hasAccess(buildScope(currentUser), document))
            throw new UnauthorizedException("You do not have access to this document");
        Path filePath = Paths.get(document.getCheminStockage());
        if (!Files.exists(filePath)) throw new ResourceNotFoundException("Physical file not found");
        document.incrementerTelechargements();
        documentRepository.save(document);
        return Files.readAllBytes(filePath);
    }

    // ─── Modifier les métadonnées ─────────────────────────────────────────────
    @Transactional
    public DocumentResponseDTO updateDocument(Long id, DocumentRequestDTO dto) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        if (!hasAccess(buildScope(currentUser), document))
            throw new UnauthorizedException("You do not have access to this document");
        boolean sender = document.getExpediteur() != null && document.getExpediteur().getId().equals(currentUser.getId());
        if (!sender && !isAdminOrManager(currentUser) && !isSuperAdmin(currentUser))
            throw new UnauthorizedException("You cannot edit this document");
        document.setNom(dto.getNom());
        document.setDescription(dto.getDescription());
        document.setType(dto.getType());
        Document saved = documentRepository.save(document);
        log.info("Document {} updated by {}", id, currentUser.getEmail());
        return documentMapper.toResponseDTO(saved);
    }

    // ─── Supprimer (soft delete) ──────────────────────────────────────────────
    @Transactional
    public void deleteDocument(Long id) throws IOException {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
        if (!hasAccess(buildScope(currentUser), document))
            throw new UnauthorizedException("You do not have access to this document");
        if (isSecretary(currentUser))
            throw new org.springframework.security.access.AccessDeniedException("Document deletion is forbidden for SECRETAIRE.");
        boolean canDelete = document.appartientA(currentUser.getId())
                || isAdminOrManager(currentUser) || isSuperAdmin(currentUser);
        if (!canDelete) throw new UnauthorizedException("You cannot delete this document");
        String chemin = document.getCheminStockage();
        document.setStatus(DocumentStatus.SUPPRIME);
        documentRepository.save(document);
        long otherRefs = documentRepository.countByCheminStockageAndStatusNot(chemin, DocumentStatus.SUPPRIME);
        if (otherRefs == 0) {
            Path filePath = Paths.get(chemin);
            if (Files.exists(filePath)) Files.delete(filePath);
        }
        log.info("Document {} deleted by {}", id, currentUser.getEmail());
    }

    // ─── Documents publics ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        DocumentScope scope = buildScope(getCurrentUser());
        return documentRepository.findPublicDocuments(DocumentStatus.ACTIF).stream()
                .filter(doc -> hasAccess(scope, doc)).map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Compteur non lus ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public long getUnreadCount(Long employerId) {
        return getUnreadDocuments(employerId).size();
    }

    // ─── Utilitaires privés ───────────────────────────────────────────────────
    private void validateFile(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty())
            throw new IllegalArgumentException("Aucun fichier fourni");
        if (fichier.getSize() > 10 * 1024 * 1024L)
            throw new IllegalArgumentException("Le fichier dépasse 10 Mo");
        String name = fichier.getOriginalFilename();
        if (name == null || !name.contains("."))
            throw new IllegalArgumentException("Nom de fichier invalide");
    }

    private Path savePhysicalFile(MultipartFile fichier) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String extension = FilenameUtils.getExtension(fichier.getOriginalFilename());
        String uniqueName = UUID.randomUUID() + "." + extension;
        Path filePath = uploadPath.resolve(uniqueName);
        Files.copy(fichier.getInputStream(), filePath);
        return filePath;
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }

    private boolean isSuperAdmin(Employer employer) {
        return employer.getRole() != null &&
                "SUPER_ADMIN".equalsIgnoreCase(employer.getRole().getName());
    }

    private boolean isAdminOrManager(Employer employer) {
        if (employer.getRole() == null) return false;
        String role = employer.getRole().getName();
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    private boolean isSecretary(Employer employer) {
        return employer != null && employer.getRole() != null && "SECRETAIRE".equals(employer.getRole().getName());
    }

    private Page<DocumentResponseDTO> listDocumentsForScope(Employer user, Pageable pageable) {
        DocumentScope scope = buildScope(user);
        Page<Document> page;
        if (scope.superAdmin()) {
            page = documentRepository.findAllByStatus(DocumentStatus.ACTIF, pageable);
        } else if (scope.admin() && scope.entrepriseId() != null) {
            page = documentRepository.findAllForEntreprise(scope.entrepriseId(), DocumentStatus.ACTIF, pageable);
        } else if (scope.manager()) {
            if (scope.entrepriseId() == null || scope.sucursalId() == null) {
                page = Page.empty(pageable);
            } else {
                List<Long> departmentIds = new ArrayList<>(scope.sucursalDepartementIds());
                if (departmentIds.isEmpty()) departmentIds.add(-1L);
                page = documentRepository.findAllForSucursal(scope.entrepriseId(), scope.sucursalId(), departmentIds,
                        DocumentStatus.ACTIF, pageable);
            }
        } else if (scope.lead()) {
            if (scope.entrepriseId() == null || scope.departementId() == null) {
                page = Page.empty(pageable);
            } else {
                page = documentRepository.findAllForDepartement(scope.entrepriseId(), scope.departementId(),
                        DocumentStatus.ACTIF, pageable);
            }
        } else if (scope.entrepriseId() != null) {
            page = documentRepository.findAllByUserAndEntreprise(user.getId(), scope.entrepriseId(),
                    DocumentStatus.ACTIF, pageable);
        } else {
            page = Page.empty(pageable);
        }
        return page.map(documentMapper::toResponseDTO);
    }

    private DocumentScope buildScope(Employer user) {
        String role = user.getRole() == null || user.getRole().getName() == null
                ? "" : user.getRole().getName().toUpperCase(Locale.ROOT);
        Long entrepriseId = user.getEntreprise() == null ? null : user.getEntreprise().getId();
        Long sucursalId = user.getSucursal() == null ? null : user.getSucursal().getId();
        Long departementId = user.getPoste() == null || user.getPoste().getDepartement() == null
                ? null : user.getPoste().getDepartement().getId();
        Set<Long> branchDepartments = new HashSet<>();
        if ("MANAGER".equals(role) && entrepriseId != null && sucursalId != null) {
            branchDepartments.addAll(employerRepository.findDepartementIdsByEntrepriseAndSucursal(entrepriseId, sucursalId));
            branchDepartments.addAll(projetRepository.findDepartementIdsByEntrepriseAndSucursal(entrepriseId, sucursalId));
        }
        return new DocumentScope(role, user.getId(), entrepriseId, sucursalId, departementId, branchDepartments);
    }

    private boolean hasAccess(DocumentScope scope, Document document) {
        if (scope.superAdmin()) return true;
        if (scope.admin()) return documentBelongsToEntreprise(document, scope.entrepriseId());
        if (scope.manager()) {
            if (!documentBelongsToEntreprise(document, scope.entrepriseId())) return false;
            return employerInSucursal(document.getExpediteur(), scope.sucursalId())
                    || employerInSucursal(document.getDestinataire(), scope.sucursalId())
                    || employerInSucursal(document.getEmployer(), scope.sucursalId())
                    || (document.getProjet() != null && sameId(document.getProjet().getSucursal(), scope.sucursalId()))
                    || employerInDepartments(document.getExpediteur(), scope.sucursalDepartementIds())
                    || employerInDepartments(document.getDestinataire(), scope.sucursalDepartementIds())
                    || employerInDepartments(document.getEmployer(), scope.sucursalDepartementIds())
                    || projectInDepartments(document, scope.sucursalDepartementIds());
        }
        if (scope.lead()) {
            if (scope.departementId() == null) return false;
            return documentBelongsToEntreprise(document, scope.entrepriseId())
                    && (employerInDepartment(document.getExpediteur(), scope.departementId())
                        || employerInDepartment(document.getDestinataire(), scope.departementId())
                        || employerInDepartment(document.getEmployer(), scope.departementId())
                        || projectInDepartments(document, Set.of(scope.departementId())));
        }
        return document.appartientA(scope.userId())
                && documentBelongsToEntreprise(document, scope.entrepriseId());
    }

    private boolean documentBelongsToEntreprise(Document d, Long entrepriseId) {
        if (entrepriseId == null) return false;
        Long ownerCompanyId = d.getExpediteur() != null && d.getExpediteur().getEntreprise() != null
                ? d.getExpediteur().getEntreprise().getId()
                : d.getEmployer() != null && d.getEmployer().getEntreprise() != null
                    ? d.getEmployer().getEntreprise().getId()
                    : d.getProjet() != null && d.getProjet().getEntreprise() != null
                        ? d.getProjet().getEntreprise().getId()
                        : d.getDestinataire() != null && d.getDestinataire().getEntreprise() != null
                            ? d.getDestinataire().getEntreprise().getId() : null;
        return Objects.equals(ownerCompanyId, entrepriseId);
    }

    private boolean employerInEntreprise(Employer employer, Long entrepriseId) {
        return employer != null && employer.getEntreprise() != null && entrepriseId != null
                && Objects.equals(employer.getEntreprise().getId(), entrepriseId);
    }

    private boolean employerInSucursal(Employer employer, Long sucursalId) {
        return employer != null && employer.getSucursal() != null && sucursalId != null
                && Objects.equals(employer.getSucursal().getId(), sucursalId);
    }

    private boolean employerInDepartment(Employer employer, Long departmentId) {
        return departmentId != null && employer != null && employer.getPoste() != null
                && employer.getPoste().getDepartement() != null
                && Objects.equals(employer.getPoste().getDepartement().getId(), departmentId);
    }

    private boolean employerInDepartments(Employer employer, Set<Long> departmentIds) {
        return employer != null && employer.getPoste() != null && employer.getPoste().getDepartement() != null
                && departmentIds.contains(employer.getPoste().getDepartement().getId());
    }

    private boolean projectInDepartments(Document document, Set<Long> departmentIds) {
        return document.getProjet() != null && document.getProjet().getDepartements() != null
                && document.getProjet().getDepartements().stream().anyMatch(d -> departmentIds.contains(d.getId()));
    }

    private boolean sameId(com.example.backend_sirhgp.model.Sucursal sucursal, Long id) {
        return sucursal != null && id != null && Objects.equals(sucursal.getId(), id);
    }

    private void verifierEmployeDansScope(DocumentScope scope, Employer employee) {
        if (isEmployerInScope(scope, employee)) return;
        throw new org.springframework.security.access.AccessDeniedException("Employe is outside your document scope.");
    }

    private boolean isEmployerInScope(DocumentScope scope, Employer employee) {
        if (employee == null) return false;
        if (scope.superAdmin()) return true;
        boolean sameCompany = employerInEntreprise(employee, scope.entrepriseId());
        if (scope.admin()) return sameCompany;
        if (scope.manager()) return sameCompany && (employerInSucursal(employee, scope.sucursalId())
                || employerInDepartments(employee, scope.sucursalDepartementIds()));
        if (scope.lead()) return sameCompany && employerInDepartment(employee, scope.departementId());
        if (isSecretaryByRole(scope.role())) return sameCompany && employerInSucursal(employee, scope.sucursalId());
        return Objects.equals(employee.getId(), scope.userId());
    }

    private boolean isProjetDansScope(DocumentScope scope, com.example.backend_sirhgp.model.Projet project) {
        if (project == null) return false;
        if (scope.superAdmin()) return true;
        if (scope.entrepriseId() == null || project.getEntreprise() == null
                || !Objects.equals(project.getEntreprise().getId(), scope.entrepriseId())) return false;
        if (scope.admin()) return true;
        if (scope.manager()) return Objects.equals(project.getSucursal() == null ? null : project.getSucursal().getId(), scope.sucursalId())
                || (project.getDepartements() != null && project.getDepartements().stream()
                    .anyMatch(d -> scope.sucursalDepartementIds().contains(d.getId())));
        if (scope.lead()) return project.getDepartements() != null && project.getDepartements().stream()
                .anyMatch(d -> Objects.equals(d.getId(), scope.departementId()));
        if (isSecretaryByRole(scope.role())) return Objects.equals(project.getSucursal() == null ? null : project.getSucursal().getId(), scope.sucursalId());
        return false;
    }

    private void verifierEmployeAccessible(DocumentScope scope, Long employeeId) {
        Employer employee = employerRepository.findByIdWithDetails(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employe not found: " + employeeId));
        verifierEmployeDansScope(scope, employee);
    }

    private boolean isSecretaryByRole(String role) { return "SECRETAIRE".equals(role); }

    private record DocumentScope(String role, Long userId, Long entrepriseId, Long sucursalId,
                                 Long departementId, Set<Long> sucursalDepartementIds) {
        private boolean superAdmin() { return "SUPER_ADMIN".equals(role); }
        private boolean admin() { return "ADMIN".equals(role); }
        private boolean manager() { return "MANAGER".equals(role); }
        private boolean lead() { return "LEAD".equals(role); }
    }

}
