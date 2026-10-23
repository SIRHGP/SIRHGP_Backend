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
import java.util.List;
import java.util.UUID;
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
        verifierPerimetreSecretaire(expediteur, destinataire);

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
            verifierPerimetreSecretaire(expediteur, lie);
            document.setEmployer(lie);
        }
        if (dto.getProjetId() != null) {
            var projet = projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé : " + dto.getProjetId()));
            if (isSecretary(expediteur) && (projet.getEntreprise() == null || projet.getSucursal() == null
                    || expediteur.getEntreprise() == null || expediteur.getSucursal() == null
                    || !expediteur.getEntreprise().getId().equals(projet.getEntreprise().getId())
                    || !expediteur.getSucursal().getId().equals(projet.getSucursal().getId()))) {
                throw new org.springframework.security.access.AccessDeniedException("Projet hors de votre succursale.");
            }
            document.setProjet(projet);
        }

        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new DocumentDispatchedEvent(
                this, saved, expediteur, destinataire, null));

        log.info("Document '{}' uploadé et dispatché par {} → {}",
                saved.getNom(), expediteur.getEmail(), destinataire.getEmail());

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

        if (isSecretary(expediteur) && (document.getExpediteur() == null
                || !expediteur.getId().equals(document.getExpediteur().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous pouvez transférer uniquement les documents que vous avez envoyés.");
        }

        if (!document.appartientA(expediteur.getId()) && !isAdminOrManager(expediteur)) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à dispatcher ce document");
        }
        verifierPerimetreSecretaire(expediteur, document.getExpediteur());

        Employer nouveauDestinataire = employerRepository.findById(dto.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé: " + dto.getDestinataireId()));

        if (isSuperAdmin(nouveauDestinataire)) {
            throw new IllegalArgumentException("Impossible de transférer un document au Super Administrateur");
        }
        verifierPerimetreSecretaire(expediteur, nouveauDestinataire);

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
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));
        if (isSecretary(current) && !doc.appartientA(current.getId())) throw new ResourceNotFoundException("Document non trouvé: " + id);
        return documentMapper.toResponseDTO(doc);
    }

    // ─── Documents reçus ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getReceivedDocuments(Long employerId) {
        verifierEmployeAccessible(getCurrentUser(), employerId);
        return documentRepository.findReceivedDocuments(employerId, DocumentStatus.ACTIF)
                .stream().map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Documents non lus ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getUnreadDocuments(Long employerId) {
        verifierEmployeAccessible(getCurrentUser(), employerId);
        return documentRepository.findUnreadReceivedDocuments(employerId, DocumentStatus.ACTIF)
                .stream().map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Documents envoyés ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getSentDocuments(Long employerId) {
        verifierEmployeAccessible(getCurrentUser(), employerId);
        return documentRepository.findSentDocuments(employerId, DocumentStatus.ACTIF)
                .stream().map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Mes documents (expéditeur OU destinataire) ───────────────────────────
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getMyDocuments(Pageable pageable) {
        Employer currentUser = getCurrentUser();
        return documentRepository.findAllByUser(currentUser.getId(), DocumentStatus.ACTIF, pageable)
                .map(documentMapper::toResponseDTO);
    }

    // ─── Tous les documents (admins voient tout, autres voient les leurs) ─────
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getAllDocuments(Pageable pageable) {
        Employer currentUser = getCurrentUser();
        if (isAdminOrManager(currentUser) || isSuperAdmin(currentUser)) {
            return documentRepository.findAllByStatus(DocumentStatus.ACTIF, pageable)
                    .map(documentMapper::toResponseDTO);
        }
        return documentRepository.findAllByUser(currentUser.getId(), DocumentStatus.ACTIF, pageable)
                .map(documentMapper::toResponseDTO);
    }

    // ─── Marquer comme lu ────────────────────────────────────────────────────
    @Transactional
    public void markAsRead(Long documentId) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

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

        if (!document.appartientA(currentUser.getId()) && !isAdminOrManager(currentUser)) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));

        boolean hasAccess = document.appartientA(currentUser.getId())
                || isAdminOrManager(currentUser)
                || isSuperAdmin(currentUser);

        if (!hasAccess) {
            throw new UnauthorizedException("Vous n'avez pas accès à ce document");
        }

        Path filePath = Paths.get(document.getCheminStockage());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Fichier physique introuvable");
        }

        document.incrementerTelechargements();
        documentRepository.save(document);
        return Files.readAllBytes(filePath);
    }

    // ─── Modifier les métadonnées ─────────────────────────────────────────────
    @Transactional
    public DocumentResponseDTO updateDocument(Long id, DocumentRequestDTO dto) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));

        boolean canEdit = document.getExpediteur().getId().equals(currentUser.getId())
                || isAdminOrManager(currentUser)
                || isSuperAdmin(currentUser);

        if (!canEdit) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier ce document");
        }
        if (isSecretary(currentUser) && !document.getExpediteur().getId().equals(currentUser.getId()))
            throw new UnauthorizedException("Vous ne pouvez modifier que les documents que vous avez envoyés.");

        document.setNom(dto.getNom());
        document.setDescription(dto.getDescription());
        document.setType(dto.getType());

        Document saved = documentRepository.save(document);
        log.info("Document {} mis à jour par {}", id, currentUser.getEmail());
        return documentMapper.toResponseDTO(saved);
    }

    // ─── Supprimer (soft delete) ──────────────────────────────────────────────
    @Transactional
    public void deleteDocument(Long id) throws IOException {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));

        if (isSecretary(currentUser)) throw new org.springframework.security.access.AccessDeniedException("La suppression des documents est interdite au rôle SECRETAIRE.");

        boolean canDelete = document.getExpediteur().getId().equals(currentUser.getId())
                || document.appartientA(currentUser.getId())
                || isAdminOrManager(currentUser)
                || isSuperAdmin(currentUser);

        if (!canDelete) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à supprimer ce document");
        }

        String chemin = document.getCheminStockage();
        document.setStatus(DocumentStatus.SUPPRIME);
        documentRepository.save(document);

        // Supprimer physiquement seulement s'il n'y a plus de référence active
        long otherRefs = documentRepository.countByCheminStockageAndStatusNot(chemin, DocumentStatus.SUPPRIME);
        if (otherRefs == 0) {
            Path filePath = Paths.get(chemin);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Fichier physique supprimé: {}", filePath);
            }
        }

        log.info("Document {} supprimé par {}", id, currentUser.getEmail());
    }

    // ─── Documents publics ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        return documentRepository.findPublicDocuments(DocumentStatus.ACTIF)
                .stream().map(documentMapper::toResponseDTO).collect(Collectors.toList());
    }

    // ─── Compteur non lus ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public long getUnreadCount(Long employerId) {
        verifierEmployeAccessible(getCurrentUser(), employerId);
        return documentRepository.countByDestinataireIdAndEstLuFalseAndStatus(employerId, DocumentStatus.ACTIF);
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

    private void verifierPerimetreSecretaire(Employer secretary, Employer employee) {
        if (!isSecretary(secretary)) return;
        if (secretary.getEntreprise() == null || secretary.getSucursal() == null
                || employee == null || employee.getEntreprise() == null || employee.getSucursal() == null
                || !secretary.getEntreprise().getId().equals(employee.getEntreprise().getId())
                || !secretary.getSucursal().getId().equals(employee.getSucursal().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("La personne doit appartenir à votre succursale.");
        }
    }

    private void verifierEmployeAccessible(Employer current, Long employeeId) {
        if (!isSecretary(current)) return;
        Employer employee = employerRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + employeeId));
        verifierPerimetreSecretaire(current, employee);
    }
}
