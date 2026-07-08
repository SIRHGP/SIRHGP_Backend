package com.example.backend_sirhgp.event;

import com.example.backend_sirhgp.model.Document;
import com.example.backend_sirhgp.model.Employer;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Événement déclenché lorsqu'un document est dispatché (envoyé) à un destinataire.
 * Permet de notifier le destinataire par email, push notification, etc.
 */
public class DocumentDispatchedEvent extends ApplicationEvent {

    private final Document document;
    private final Employer expediteur;
    private final Employer destinataire;
    private final String message;
    private final LocalDateTime dispatchedAt;

    public DocumentDispatchedEvent(Object source,
                                   Document document,
                                   Employer expediteur,
                                   Employer destinataire,
                                   String message) {
        super(source);
        this.document = document;
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.message = message;
        this.dispatchedAt = LocalDateTime.now();
    }

    public Document getDocument() {
        return document;
    }

    public Employer getExpediteur() {
        return expediteur;
    }

    public Employer getDestinataire() {
        return destinataire;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    @Override
    public String toString() {
        return String.format("DocumentDispatchedEvent{document=%s, expediteur=%s, destinataire=%s, dispatchedAt=%s}",
                document.getNom(), expediteur.getEmail(), destinataire.getEmail(), dispatchedAt);
    }
}
