package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class DepartementRequestDTO {
    @NotBlank(message = "Le nom du département est obligatoire")
    @Size(max = 100)
    private String name;

    private Set<Long> projetIds;
}