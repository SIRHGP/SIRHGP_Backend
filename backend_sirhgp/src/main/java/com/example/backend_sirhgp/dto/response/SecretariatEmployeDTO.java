package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SecretariatEmployeDTO {
    private Long id;
    private String name;
    private String email;
    private String posteName;
    private LocalDateTime heureArrivee;
    private LocalDateTime heureDepart;
}
