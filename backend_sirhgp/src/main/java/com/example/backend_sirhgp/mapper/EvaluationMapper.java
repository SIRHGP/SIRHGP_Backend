// ─── EvaluationMapper.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.request.CritereEvaluationDTO;
import com.example.backend_sirhgp.dto.request.EvaluationRequestDTO;
import com.example.backend_sirhgp.dto.response.EvaluationResponseDTO;
import com.example.backend_sirhgp.model.CritereEvaluation;
import com.example.backend_sirhgp.model.Evaluation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Mapper du module Évaluations (2.4) — manuel (pattern RecrutementMappers). */
@Component
public class EvaluationMapper {

    public CritereEvaluationDTO toCritereDTO(CritereEvaluation c) {
        if (c == null) return null;
        CritereEvaluationDTO dto = new CritereEvaluationDTO();
        dto.setLibelle(c.getLibelle());
        dto.setNote(c.getNote());
        dto.setCoefficient(c.getCoefficient());
        return dto;
    }

    public EvaluationResponseDTO toResponseDTO(Evaluation e) {
        if (e == null) return null;
        EvaluationResponseDTO dto = new EvaluationResponseDTO();
        dto.setId(e.getId());
        dto.setType(e.getType());
        dto.setCategorie(e.getCategorie());
        dto.setObjet(e.getObjet());
        dto.setAnnee(e.getAnnee());
        dto.setPeriode(e.getPeriode());
        dto.setStatut(e.getStatut());
        dto.setDateEvaluation(e.getDateEvaluation());
        dto.setCommentaire(e.getCommentaire());
        dto.setNoteGlobale(e.getNoteGlobale());
        dto.setRapport(e.getRapport());
        dto.setDateRapport(e.getDateRapport());
        if (e.getEvalue() != null) {
            dto.setEvalueId(e.getEvalue().getId());
            dto.setEvalueName(e.getEvalue().getName());
            // Rôle de l'évalué (fetch-join sur evalue, accès au rôle en transaction)
            dto.setEvalueRole(e.getEvalue().getRole() != null ? e.getEvalue().getRole().getName() : null);
        }
        if (e.getEvaluateur() != null) {
            dto.setEvaluateurId(e.getEvaluateur().getId());
            dto.setEvaluateurName(e.getEvaluateur().getName());
            dto.setEvaluateurRole(e.getEvaluateur().getRole() != null ? e.getEvaluateur().getRole().getName() : null);
        }
        if (e.getCriteres() != null) {
            List<CritereEvaluationDTO> criteres = e.getCriteres().stream()
                    .map(this::toCritereDTO)
                    .collect(Collectors.toList());
            dto.setCriteres(criteres);
        } else {
            dto.setCriteres(Collections.emptyList());
        }
        return dto;
    }

    /** Construit un critère (côté entité) depuis le DTO. */
    public CritereEvaluation toEntity(CritereEvaluationDTO dto, Evaluation evaluation) {
        CritereEvaluation c = new CritereEvaluation();
        c.setLibelle(dto.getLibelle());
        c.setNote(dto.getNote());
        c.setCoefficient(dto.getCoefficient() != null ? dto.getCoefficient() : java.math.BigDecimal.ONE);
        c.setEvaluation(evaluation);
        return c;
    }
}