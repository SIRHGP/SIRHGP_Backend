package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.CommentaireRequestDTO;
import com.example.backend_sirhgp.dto.response.CommentaireResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.CommentaireMapper;
import com.example.backend_sirhgp.model.CommentaireTache;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.repository.CommentaireTacheRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CORRECTIONS :
 * - auteurId JAMAIS reçu du client — extrait du SecurityContext (JWT).
 *   Un utilisateur ne peut pas poster un commentaire au nom d'un autre.
 * - deleteByTache(Tache) remplacé par deleteByTacheId(Long).
 * - findByTacheId() utilise la query avec JOIN FETCH sur auteur.
 * - Vérification de propriété sur update/delete : seul l'auteur ou un admin
 *   peut modifier/supprimer son commentaire.
 * - CommentaireRequestDTO / CommentaireResponseDTO au lieu de l'ancien DTO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireTacheRepository commentaireRepository;
    private final TacheRepository tacheRepository;
    private final EmployerRepository employerRepository;
    private final CommentaireMapper commentaireMapper;

    @Transactional
    public CommentaireResponseDTO create(CommentaireRequestDTO dto) {
        // CORRECTION : auteur extrait du JWT, pas du body
        Employer auteur = getCurrentUser();

        Tache tache = tacheRepository.findById(dto.getTacheId())
                .orElseThrow(() -> new ResourceNotFoundException("Tâche non trouvée : " + dto.getTacheId()));

        CommentaireTache commentaire = CommentaireTache.builder()
                .contenu(dto.getContenu())
                .tache(tache)
                .auteur(auteur)
                .estModifie(false)
                .build();

        CommentaireTache saved = commentaireRepository.save(commentaire);
        log.info("Commentaire créé sur tâche {} par {}", saved.getTache().getId(), auteur.getEmail());
        return commentaireMapper.toResponseDTO(saved);
    }

    public List<CommentaireResponseDTO> findByTacheId(Long tacheId) {
        // CORRECTION : utilise la query avec JOIN FETCH pour éviter N+1 sur auteur
        return commentaireRepository.findByTacheIdWithAuteur(tacheId).stream()
                .map(commentaireMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<CommentaireResponseDTO> findRecentsByTacheId(Long tacheId, int limit) {
        return commentaireRepository
                .findRecentsByTacheId(tacheId, PageRequest.of(0, limit)).stream()
                .map(commentaireMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public CommentaireResponseDTO findById(Long id) {
        CommentaireTache commentaire = commentaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé : " + id));
        return commentaireMapper.toResponseDTO(commentaire);
    }

    @Transactional
    public CommentaireResponseDTO update(Long id, String nouveauContenu) {
        CommentaireTache commentaire = commentaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé : " + id));

        // CORRECTION : vérification de propriété — seul l'auteur peut modifier
        Employer currentUser = getCurrentUser();
        if (!commentaire.getAuteur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres commentaires");
        }

        // Utilise la méthode métier qui gère estModifie = true
        commentaire.mettreAJourContenu(nouveauContenu);
        CommentaireTache updated = commentaireRepository.save(commentaire);
        return commentaireMapper.toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        CommentaireTache commentaire = commentaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire non trouvé : " + id));

        // CORRECTION : seul l'auteur ou un ADMIN peut supprimer
        Employer currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!commentaire.getAuteur().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres commentaires");
        }

        commentaireRepository.deleteById(id);
        log.info("Commentaire {} supprimé par {}", id, currentUser.getEmail());
    }

    @Transactional
    public void deleteByTacheId(Long tacheId) {
        // CORRECTION : deleteByTacheId au lieu de deleteByTache(Tache)
        commentaireRepository.deleteByTacheId(tacheId);
        log.info("Tous les commentaires de la tâche {} supprimés", tacheId);
    }

    // ─── Utilitaire ──────────────────────────────────────────

    private Employer getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return employerRepository.findByEmailWithRolesAndPoste(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur courant non trouvé"));
    }
}