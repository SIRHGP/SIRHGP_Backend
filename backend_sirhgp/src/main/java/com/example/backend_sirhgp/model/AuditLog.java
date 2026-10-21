package com.example.backend_sirhgp.model;

import com.example.backend_sirhgp.audit.AuditActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité représentant un journal d'audit : trace chaque action effectuée
 * sur la plateforme (qui, quoi, quand, où, impacts).
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action_type"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** ID de l'utilisateur qui a effectué l'action (peut être null si connexion échouée) */
    @Column(name = "user_id")
    private Long userId;

    /** Email de l'utilisateur qui a effectué l'action */
    @Column(name = "user_email", length = 150)
    private String userEmail;

    /** Nom de l'utilisateur */
    @Column(name = "user_name", length = 150)
    private String userName;

    /** Rôle de l'utilisateur au moment de l'action */
    @Column(name = "user_role", length = 50)
    private String userRole;

    /** Type d'action (CREATE, UPDATE, DELETE, LOGIN, ...) */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private AuditActionType actionType;

    /** Description lisible de l'action */
    @Column(name = "action_description", columnDefinition = "TEXT")
    private String actionDescription;

    /** Nom de l'entité affectée (ex: Employer, Tache, Document...) */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** ID de l'entité affectée */
    @Column(name = "entity_id")
    private Long entityId;

    /** Nom de la ressource / entité (ex: nom de l'employé, titre de la tâche) */
    @Column(name = "entity_name", length = 255)
    private String entityName;

    /** Méthode HTTP (GET, POST, PUT, PATCH, DELETE) */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /** Chemin de la requête (ex: /api/employers/12) */
    @Column(name = "request_path", length = 500)
    private String requestPath;

    /** Paramètres de requête sérialisés (JSON) */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /** Données de la requête (body sérialisé) */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /** Code de statut HTTP de la réponse */
    @Column(name = "http_status")
    private int httpStatus;

    /** Adresse IP du client */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent du navigateur/client */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** Succès de l'action */
    @Column(name = "success", nullable = false)
    private boolean success;

    /** Message d'erreur en cas d'échec */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Durée d'exécution en millisecondes */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** Horodatage de l'action */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}