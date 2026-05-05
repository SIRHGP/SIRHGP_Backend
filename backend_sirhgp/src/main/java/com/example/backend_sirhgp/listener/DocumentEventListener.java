package com.example.backend_sirhgp.listener;


import com.example.backend_sirhgp.event.DocumentDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour les événements liés aux documents.
 * Gère les notifications asynchrones lors du dispatch des documents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventListener {

    /**
     * Gère l'événement de dispatch d'un document.
     * Cette méthode est exécutée de manière asynchrone pour ne pas bloquer l'opération principale.
     */
    @Async
    @EventListener
    public void handleDocumentDispatched(DocumentDispatchedEvent event) {
        log.info("📄 Nouveau document reçu par {} de la part de {} : {}",
                event.getDestinataire().getEmail(),
                event.getExpediteur().getEmail(),
                event.getDocument().getNom());

        // TODO: Implémenter les notifications:
        // 1. Envoi d'email au destinataire
        // 2. Push notification (WebSocket)
        // 3. Notification dans la BDD
        // 4. Webhook vers des services externes

        sendEmailNotification(event);
        createInAppNotification(event);
    }

    /**
     * Envoie une notification par email au destinataire.
     */
    private void sendEmailNotification(DocumentDispatchedEvent event) {
        // À implémenter avec un service d'email (JavaMailSender)
        log.debug("📧 Email notification à envoyer à {} pour le document {}",
                event.getDestinataire().getEmail(),
                event.getDocument().getNom());

        // Exemple de contenu d'email:
        // Sujet: "Nouveau document reçu de la part de {expediteur.nom}"
        // Corps:
        // "Bonjour {destinataire.nom},
        //
        //  Vous avez reçu un nouveau document de la part de {expediteur.nom}.
        //
        //  Document: {document.nom}
        //  Message: {event.message}
        //
        //  Téléchargez-le ici: /api/documents/{document.id}/download"
    }

    /**
     * Crée une notification in-app pour le destinataire.
     */
    private void createInAppNotification(DocumentDispatchedEvent event) {
        // À implémenter avec un service de notifications
        log.debug("🔔 Notification in-app créée pour {} (document: {})",
                event.getDestinataire().getEmail(),
                event.getDocument().getNom());

        // Stocker dans une table Notification pour affichage dans l'interface
    }
}
