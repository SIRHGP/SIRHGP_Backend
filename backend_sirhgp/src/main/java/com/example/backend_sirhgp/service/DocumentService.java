package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.DocumentRequestDTO;
import com.example.backend_sirhgp.dto.request.DocumentTransferRequestDTO;
import com.example.backend_sirhgp.dto.response.DocumentPartageResponseDTO;
import com.example.backend_sirhgp.dto.response.DocumentResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.DocumentMapper;
import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.DocumentPartage;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.repository.DocumentPartageRepository;
import com.example.backend_sirhgp.repository.DocumentRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository         documentRepository;
    private final DocumentPartageRepository  documentPartageRepository;
    private final EmployerRepository         employerRepository;
    private final ProjetRepository           projetRepository;
    private final DocumentMapper             documentMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ── Upload ───────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponseDTO upload(DocumentRequestDTO dto,
                                      MultipartFile fichier) throws IOException {
        validateFile(fichier);

        Employer uploader = getCurrentUser();

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String extension     = FilenameUtils.getExtension(fichier.getOriginalFilename());
        String uniqueName    = UUID.randomUUID() + "." + extension;
        Path   filePath      = uploadPath.resolve(uniqueName);
        Files.copy(fichier.getInputStream(), filePath);

        Document doc = Document.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .type(dto.getType())
                .publique(dto.isPublique())
                .nomFichier(uniqueName)
                .cheminStockage(filePath.toAbsolutePath().toString())
                .typeMime(fichier.getContentType())
                .taille(fichier.getSize())
                .uploader(uploader)
                .nombreTelechargements(0L)
                .build();

        if (dto.getEmployerId() != null) {
            doc.setEmployer(employerRepository.findById(dto.getEmployerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employé non trouvé : " + dto.getEmployerId())));
        }
        if (dto.getProjetId() != null) {
            doc.setProjet(projetRepository.findById(dto.getProjetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Projet non trouvé : " + dto.getProjetId())));
        }

        Document saved = documentRepository.save(doc);
        log.info("Document uploadé : {} par {}", saved.getNom(), uploader.getEmail());
        return enrich(documentMapper.toResponseDTO(saved), saved);
    }

    // ── Transfer ─────────────────────────────────────────────────────────────

    /**
     * Transfère un document existant à un autre employé.
     * Seuls l'expéditeur et le destinataire pourront accéder à ce document.
     */
    @Transactional
    public DocumentPartageResponseDTO transfer(Long documentId,
                                               DocumentTransferRequestDTO dto) {
        Employer expediteur = getCurrentUser();

        Document document = documentRepository.findByIdWithRelations(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé : " + documentId));

        if (expediteur.getId().equals(dto.getRecipientId())) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas vous transférer un document à vous-même");
        }

        Employer destinataire = employerRepository.findById(dto.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destinataire non trouvé : " + dto.getRecipientId()));

        DocumentPartage partage = DocumentPartage.builder()
                .document(document)
                .expediteur(expediteur)
                .destinataire(destinataire)
                .message(dto.getMessage())
                .lu(false)
                .build();

        DocumentPartage saved = documentPartageRepository.save(partage);
        log.info("Document {} transféré de {} à {}",
                documentId, expediteur.getEmail(), destinataire.getEmail());

        return toPartageDTO(saved);
    }

    // ── Partages reçus (pour le destinataire) ────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentPartageResponseDTO> getPartagesRecus(Long employerId) {
        return documentPartageRepository
                .findByDestinataireIdOrderByDateEnvoiDesc(employerId)
                .stream()
                .map(this::toPartageDTO)
                .collect(Collectors.toList());
    }

    // ── Partages envoyés (pour l'expéditeur) ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentPartageResponseDTO> getPartagesEnvoyes(Long employerId) {
        return documentPartageRepository
                .findByExpediteurIdOrderByDateEnvoiDesc(employerId)
                .stream()
                .map(this::toPartageDTO)
                .collect(Collectors.toList());
    }

    // ── Marquer comme lu ─────────────────────────────────────────────────────

    @Transactional
    public void marquerLu(Long documentId) {
        Employer current = getCurrentUser();
        documentPartageRepository.marquerLus(current.getId(), documentId);
    }

    // ── findAll (admin voit tout, sinon filtré) ───────────────────────────────

    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> findAll(Pageable pageable) {
        return documentRepository.findAll(pageable)
                .map(doc -> enrich(documentMapper.toResponseDTO(doc), doc));
    }

    // ── findByEmployerId ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> findByEmployerId(Long employerId) {
        return documentRepository.findByEmployerId(employerId).stream()
                .map(doc -> enrich(documentMapper.toResponseDTO(doc), doc))
                .collect(Collectors.toList());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DocumentResponseDTO findById(Long id) {
        Document doc = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé : " + id));
        return enrich(documentMapper.toResponseDTO(doc), doc);
    }

    // ── findByProjetId ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> findByProjetId(Long projetId) {
        return documentRepository.findByProjetId(projetId).stream()
                .map(doc -> enrich(documentMapper.toResponseDTO(doc), doc))
                .collect(Collectors.toList());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponseDTO update(Long id, DocumentRequestDTO dto) {
        Document doc = documentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé : " + id));

        documentMapper.updateMetadonnees(doc, dto.getNom(),
                dto.getDescription(), dto.isPublique());

        if (dto.getEmployerId() != null) {
            doc.setEmployer(employerRepository.findById(dto.getEmployerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employé non trouvé : " + dto.getEmployerId())));
        }
        doc.setProjet(dto.getProjetId() != null
                ? projetRepository.findById(dto.getProjetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet non trouvé : " + dto.getProjetId()))
                : null);

        return enrich(documentMapper.toResponseDTO(documentRepository.save(doc)), doc);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) throws IOException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé : " + id));

        // Supprimer tous les partages liés avant de supprimer le document
        documentPartageRepository.findByDocumentId(id)
                .forEach(documentPartageRepository::delete);

        Path filePath = Paths.get(doc.getCheminStockage());
        if (Files.exists(filePath)) Files.delete(filePath);

        documentRepository.delete(doc);
        log.info("Document supprimé : {}", id);
    }

    // ── download ──────────────────────────────────────────────────────────────

    @Transactional
    public byte[] download(Long id) throws IOException {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé : " + id));

        Path filePath = Paths.get(doc.getCheminStockage());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException(
                    "Fichier physique introuvable pour le document : " + id);
        }

        doc.incrementerTelechargements();
        documentRepository.save(doc);
        return Files.readAllBytes(filePath);
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private void validateFile(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty())
            throw new IllegalArgumentException("Aucun fichier fourni");
        if (fichier.getSize() > 10 * 1024 * 1024L)
            throw new IllegalArgumentException(
                    "Le fichier dépasse la taille maximale de 10 Mo");
        String name = fichier.getOriginalFilename();
        if (name == null || !name.contains("."))
            throw new IllegalArgumentException("Nom de fichier invalide");
    }

    private DocumentResponseDTO enrich(DocumentResponseDTO dto, Document doc) {
        dto.setExtension(FilenameUtils.getExtension(doc.getNomFichier()));
        if (doc.getTaille() != null)
            dto.setTailleFormatee(FileUtils.byteCountToDisplaySize(doc.getTaille()));
        return dto;
    }

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur courant non trouvé"));
    }

    private DocumentPartageResponseDTO toPartageDTO(DocumentPartage p) {
        DocumentPartageResponseDTO dto = new DocumentPartageResponseDTO();
        dto.setId(p.getId());

        // Document
        dto.setDocumentId(p.getDocument().getId());
        dto.setDocumentNom(p.getDocument().getNom());
        dto.setDocumentType(p.getDocument().getType() != null
                ? p.getDocument().getType().name() : null);
        dto.setDocumentTaille(p.getDocument().getTaille());
        dto.setDocumentTypeMime(p.getDocument().getTypeMime());

        // Expéditeur
        dto.setExpediteurId(p.getExpediteur().getId());
        dto.setExpediteurNom(p.getExpediteur().getName());
        dto.setExpediteurEmail(p.getExpediteur().getEmail());

        // Destinataire
        dto.setDestinataireId(p.getDestinataire().getId());
        dto.setDestinataireNom(p.getDestinataire().getName());
        dto.setDestinataireEmail(p.getDestinataire().getEmail());

        dto.setMessage(p.getMessage());
        dto.setLu(p.isLu());
        dto.setDateEnvoi(p.getDateEnvoi());
        return dto;
    }
}