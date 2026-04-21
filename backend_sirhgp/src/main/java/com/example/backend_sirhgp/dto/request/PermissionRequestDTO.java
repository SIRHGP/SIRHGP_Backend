package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class PermissionRequestDTO {
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String name;
    private String description;
    private Set<Long> actionIds;
}