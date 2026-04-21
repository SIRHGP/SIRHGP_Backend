package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.event.TaskAssignedEvent;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Tache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void handleTaskAssigned(TaskAssignedEvent event) {
        Tache task = event.getTask();
        Employer assignee = event.getAssignee();
        Employer assignedBy = event.getAssignedBy();

        // Construire le message en fonction du rôle du destinataire
        String messageContent = buildMessageForRole(task, assignee.getRole().getName(), assignedBy);

        // 1. Envoi email (asynchrone)
        sendEmail(assignee.getEmail(), "Nouvelle tâche assignée : " + task.getTitre(), messageContent);

        // 2. Envoi notification WebSocket temps réel
        messagingTemplate.convertAndSendToUser(
                assignee.getEmail(),
                "/queue/notifications",
                new NotificationPayload(task.getId(), task.getTitre(), messageContent)
        );

        log.info("Notification envoyée à {} pour la tâche {} (rôle: {})", assignee.getEmail(), task.getId(), assignee.getRole().getName());
    }

    private String buildMessageForRole(Tache task, String roleName, Employer assignedBy) {
        String base = "Vous avez été assigné à la tâche : " + task.getTitre();
        switch (roleName) {
            case "STAGIAIRE":
                return base + ". Connectez-vous pour voir les détails.";
            case "STAFF":
                return base + "\nÉchéance : " + (task.getDateEcheance() != null ? task.getDateEcheance() : "non définie") +
                        "\nPriorité : " + task.getPriorite();
            case "MANAGER":
            case "ADMIN":
            case "SUPER_ADMIN":
                return base + "\nProjet : " + task.getProjet().getNom() +
                        "\nÉchéance : " + (task.getDateEcheance() != null ? task.getDateEcheance() : "non définie") +
                        "\nPriorité : " + task.getPriorite() +
                        "\nAssignée par : " + assignedBy.getName() +
                        "\nDescription : " + (task.getDescription() != null ? task.getDescription() : "aucune");
            default:
                return base;
        }
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@sirhgp.com"); // À configurer
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur envoi email à {} : {}", to, e.getMessage());
        }
    }

    // Payload simple pour WebSocket
    static class NotificationPayload {
        public Long taskId;
        public String title;
        public String message;
        public NotificationPayload(Long taskId, String title, String message) {
            this.taskId = taskId;
            this.title = title;
            this.message = message;
        }
    }
}