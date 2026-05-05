package com.example.backend_sirhgp.event;

import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.Employer;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class FactureDispatchedEvent extends ApplicationEvent {

    private final Facture facture;
    private final Employer emetteur;
    private final Employer client;
    private final String message;
    private final LocalDateTime dispatchedAt;

    public FactureDispatchedEvent(Object source,
                                  Facture facture,
                                  Employer emetteur,
                                  Employer client,
                                  String message) {
        super(source);
        this.facture = facture;
        this.emetteur = emetteur;
        this.client = client;
        this.message = message;
        this.dispatchedAt = LocalDateTime.now();
    }

    public Facture getFacture() {
        return facture;
    }

    public Employer getEmetteur() {
        return emetteur;
    }

    public Employer getClient() {
        return client;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    @Override
    public String toString() {
        return String.format("FactureDispatchedEvent{facture=%s, emetteur=%s, client=%s, dispatchedAt=%s}",
                facture.getNumeroFacture(), emetteur.getEmail(), client.getEmail(), dispatchedAt);
    }
}