package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refonte 5.9 (§5) — quotas par entreprise (null = illimité).
 * Enforcement : register-entreprise, EmployerService.create, SucursalService.create,
 * DocumentService.uploadWithDispatch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResponseDTO {
    private Long entrepriseId; // null = quota global par défaut
    private String entrepriseNom;
    private Integer maxEmployes;
    private Integer maxSucursales;
    private Long maxStockageMo;
}