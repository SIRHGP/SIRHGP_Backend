package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class PosteRequestDTO {
    @NotBlank(message = "Le nom du poste est obligatoire")
    @Size(max = 100)
    private String name;
    private String description;

    private Long roleId;

    @NotNull(message = "Le département est obligatoire")
    private Long departementId;

    private Set<Long> permissionIds;
}
