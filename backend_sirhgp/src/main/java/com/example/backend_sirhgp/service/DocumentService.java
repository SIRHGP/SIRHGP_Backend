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
import org.apache.commons.io.FileUtils;
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

    // ─── Upload avec dispatch immédiat (optionnel) ─────────────────────────────

    @Transactional
    public DocumentResponseDTO uploadWithDispatch(DocumentRequestDTO dto,
                                                  MultipartFile fichier) throws IOException {
        Employer expediteur = getCurrentUser();
        validateFile(fichier);

        // Sauvegarde physique
        Path filePath = savePhysicalFile(fichier);
        String uniqueName = filePath.getFileName().toString();

        // Construction du document
        Document.DocumentBuilder builder = Document.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .type(dto.getType())
                .publique(dto.isPublique())
                .nomFichier(uniqueName)
                .cheminStockage(filePath.toAbsolutePath().toString())
                .typeMime(fichier.getContentType())
                .taille(fichier.getSize())
                .expediteur(expediteur)
                .nombreTelechargements(0L)
                .status(DocumentStatus.ACTIF);

        Employer destinataire = null;

        // Si un destinataire est spécifié, dispatch immédiat
        if (dto.getDestinataireId() != null) {
            destinataire = employerRepository.findById(dto.getDestinataireId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé: " + dto.getDestinataireId()));

            // Vérification: ne pas envoyer au super admin
            if (isSuperAdmin(destinataire)) {
                throw new IllegalArgumentException("Impossible d'envoyer un document au Super Administrateur");
            }

            builder.destinataire(destinataire)
                    .message(dto.getMessage())
                    .dateEnvoi(LocalDateTime.now());
        }

        // Associations optionnelles
        if (dto.getEmployerId() != null) {
            builder.employer(employerRepository.findById(dto.getEmployerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé: " + dto.getEmployerId())));
        }
        if (dto.getProjetId() != null) {
            builder.projet(projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé: " + dto.getProjetId())));
        }

        Document saved = documentRepository.save(builder.build());

        // Publier l'événement de dispatch si un destinataire existe
        if (destinataire != null) {
            eventPublisher.publishEvent(new DocumentDispatchedEvent(
                    this, saved, expediteur, destinataire, dto.getMessage()));
            log.info("📨 Événement de dispatch publié pour le document {} vers {}",
                    saved.getNom(), destinataire.getEmail());
        }

        log.info("Document uploadé et dispatché: {} par {} -> {}",
                saved.getNom(), expediteur.getEmail(),
                dto.getDestinataireId() != null ? "destinataire " + dto.getDestinataireId() : "aucun destinataire");

        return documentMapper.toResponseDTO(saved);
    }

    // ─── Transfert d'un document existant vers un autre employé ───────────────

    @Transactional
    public DocumentResponseDTO transferDocument(Long documentId, DocumentTransferRequestDTO dto) {
        Employer expediteur = getCurrentUser();

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        // Vérifier que l'utilisateur est l'expéditeur ou le destinataire actuel
        if (!document.appartientA(expediteur.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à transférer ce document");
        }

        Employer nouveauDestinataire = employerRepository.findById(dto.getDestinataireId())
                .orElseThrow(() -> new ResourceNotFoundException("Destinataire non trouvé: " + dto.getDestinataireId()));

        // Vérification: ne pas envoyer au super admin
        if (isSuperAdmin(nouveauDestinataire)) {
            throw new IllegalArgumentException("Impossible de transférer un document au Super Administrateur");
        }

        // Créer une nouvelle instance du document pour le destinataire
        Document newDocument = Document.builder()
                .nom(document.getNom())
                .description(document.getDescription())
                .type(document.getType())
                .publique(false)
                .nomFichier(document.getNomFichier())
                .cheminStockage(document.getCheminStockage())
                .typeMime(document.getTypeMime())
                .taille(document.getTaille())
                .expediteur(expediteur)
                .destinataire(nouveauDestinataire)
                .message(dto.getMessage())
                .employer(document.getEmployer())
                .projet(document.getProjet())
                .nombreTelechargements(0L)
                .status(DocumentStatus.ACTIF)
                .dateEnvoi(LocalDateTime.now())
                .build();

        Document saved = documentRepository.save(newDocument);

        // Publier l'événement de dispatch
        eventPublisher.publishEvent(new DocumentDispatchedEvent(
                this, saved, expediteur, nouveauDestinataire, dto.getMessage()));

        log.info("Document {} transféré de {} à {}", documentId, expediteur.getEmail(), nouveauDestinataire.getEmail());

        return documentMapper.toResponseDTO(saved);
    }

    // ─── Récupérer les métadonnées d'un document ──────────────────────────────

    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentMetadata(Long id) {
        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));
        return documentMapper.toResponseDTO(document);
    }

    // ─── Récupérer les documents reçus par un employé ─────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getReceivedDocuments(Long employerId) {
        return documentRepository.findReceivedDocuments(employerId, DocumentStatus.ACTIF)
                .stream()
                .map(documentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Récupérer les documents non lus ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getUnreadDocuments(Long employerId) {
        return documentRepository.findUnreadReceivedDocuments(employerId, DocumentStatus.ACTIF)
                .stream()
                .map(documentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Récupérer les documents envoyés par un employé ───────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getSentDocuments(Long employerId) {
        return documentRepository.findSentDocuments(employerId, DocumentStatus.ACTIF)
                .stream()
                .map(documentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Récupérer tous les documents d'un utilisateur (avec pagination) ─────

    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getMyDocuments(Pageable pageable) {
        Employer currentUser = getCurrentUser();
        return documentRepository.findAllByUser(currentUser.getId(), DocumentStatus.ACTIF, pageable)
                .map(documentMapper::toResponseDTO);
    }

    // ─── Marquer un document comme lu ─────────────────────────────────────────

    @Transactional
    public void markAsRead(Long documentId) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        if (document.getDestinataire() == null || !document.getDestinataire().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le destinataire de ce document");
        }

        document.marquerCommeLu();
        documentRepository.save(document);
        log.info("Document {} marqué comme lu par {}", documentId, currentUser.getEmail());
    }

    // ─── Archiver un document ─────────────────────────────────────────────────

    @Transactional
    public void archiveDocument(Long documentId) {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + documentId));

        if (!document.appartientA(currentUser.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à archiver ce document");
        }

        document.setEstArchive(true);
        documentRepository.save(document);
        log.info("Document {} archivé par {}", documentId, currentUser.getEmail());
    }

    // ─── Télécharger un document (avec incrément du compteur) ─────────────────

    @Transactional
    public byte[] downloadDocument(Long id) throws IOException {
        Employer currentUser = getCurrentUser();

        Document document = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));

        // Vérifier l'accès
        if (!document.isPublique() && !document.appartientA(currentUser.getId())) {
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

    // ─── Supprimer un document (soft delete) ──────────────────────────────────

    @Transactional
    public void deleteDocument(Long id) throws IOException {
        Employer currentUser = getCurrentUser();
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé: " + id));

        // Seul l'expéditeur peut supprimer définitivement
        if (!document.getExpediteur().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Seul l'expéditeur peut supprimer ce document");
        }

        document.setStatus(DocumentStatus.SUPPRIME);
        documentRepository.save(document);

        // Supprimer le fichier physique si plus aucune référence active
        boolean hasOtherReferences = documentRepository.findAll().stream()
                .anyMatch(d -> d.getCheminStockage().equals(document.getCheminStockage())
                        && d.getStatus() != DocumentStatus.SUPPRIME);

        if (!hasOtherReferences) {
            Path filePath = Paths.get(document.getCheminStockage());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Fichier physique supprimé: {}", filePath);
            }
        }

        log.info("Document {} supprimé par {}", id, currentUser.getEmail());
    }

    // ─── Récupérer les documents publics ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        return documentRepository.findPublicDocuments(DocumentStatus.ACTIF)
                .stream()
                .map(documentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── Récupérer le nombre de documents non lus ─────────────────────────────

    @Transactional(readOnly = true)
    public long getUnreadCount(Long employerId) {
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
}