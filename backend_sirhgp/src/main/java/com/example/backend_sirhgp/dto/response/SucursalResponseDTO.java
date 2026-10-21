// ─── SucursalResponseDTO.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SucursalResponseDTO {

    private Long id;
    private String name;
    private String ville;
    private String adresse;
    private String telephone;
    private String statut;
    private Long entrepriseId;
    private String entrepriseNom;

    // Compteur enrichi (facultatif)
    private Long nombreEmployes;

    // 5.9 : manager affecté à la sucursal (FK inverse Sucursal.manager_id)
    private Long managerId;
    private String managerNom;

    private LocalDateTime dateCreation;
}