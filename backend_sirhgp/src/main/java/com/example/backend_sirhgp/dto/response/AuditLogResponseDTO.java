package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.audit.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private String userRole;
    private AuditActionType actionType;
    private String actionLabel;
    private String actionDescription;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String httpMethod;
    private String requestPath;
    private int httpStatus;
    private String ipAddress;
    private String userAgent;
    private boolean success;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime timestamp;
}
