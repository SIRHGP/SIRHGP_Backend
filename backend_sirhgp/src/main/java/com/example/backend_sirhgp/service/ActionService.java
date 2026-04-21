package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.request.ActionRequestDTO;
import com.example.backend_sirhgp.dto.response.ActionResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.ActionMapper;
import com.example.backend_sirhgp.model.Action;
import com.example.backend_sirhgp.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CORRECTIONS :
 * - ActionRequestDTO / ActionResponseDTO au lieu de l'ancien ActionDTO.
 * - actionMapper.toEntity() / toDTO() remplacés par toResponseDTO() et
 *   construction manuelle depuis le RequestDTO (le mapper corrigé n'expose
 *   plus toEntity() sur Action — trop simple pour justifier un mapper).
 * - findAll() paginé → Page<ActionResponseDTO> pour éviter le chargement
 *   de toute la table en mémoire.
 * - Messages d'erreur en français (cohérence avec le reste du projet).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;

    @Transactional
    public ActionResponseDTO create(ActionRequestDTO dto) {
        if (actionRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Une action avec ce nom existe déjà : " + dto.getName());
        }
        // Construction manuelle : Action est trop simple pour un mapper toEntity()
        Action action = Action.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .build();
        action = actionRepository.save(action);
        log.info("Action créée : {}", action.getName());
        return actionMapper.toResponseDTO(action);
    }

    // CORRECTION : pagination pour éviter le chargement complet de la table
    public Page<ActionResponseDTO> findAll(Pageable pageable) {
        return actionRepository.findAll(pageable)
                .map(actionMapper::toResponseDTO);
    }

    public ActionResponseDTO findById(Long id) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée : " + id));
        return actionMapper.toResponseDTO(action);
    }

    @Transactional
    public ActionResponseDTO update(Long id, ActionRequestDTO dto) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée : " + id));

        if (dto.getName() != null && !dto.getName().equals(action.getName())) {
            if (actionRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Ce nom d'action est déjà utilisé");
            }
            action.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            action.setDescription(dto.getDescription());
        }
        action = actionRepository.save(action);
        return actionMapper.toResponseDTO(action);
    }

    @Transactional
    public void delete(Long id) {
        if (!actionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Action non trouvée : " + id);
        }
        actionRepository.deleteById(id);
        log.info("Action supprimée : {}", id);
    }
}