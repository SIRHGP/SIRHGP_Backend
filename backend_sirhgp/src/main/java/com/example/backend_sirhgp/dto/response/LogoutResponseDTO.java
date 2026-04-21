package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LogoutResponseDTO {
    private String email;
    private String message;
    private LocalDateTime timestamp;
}