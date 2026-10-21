// ─── SucursalRequestDTO.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Sucursal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SucursalRequestDTO {

    @NotBlank(message = "Le nom de la sucursal est obligatoire")
    @Size(max = 255)
    private String name;

    private String ville;
    private String adresse;
    private String telephone;
    private Sucursal.StatutSucursal statut; // optionnel (défaut ACTIVE)
}