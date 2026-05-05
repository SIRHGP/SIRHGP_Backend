package com.example.backend_sirhgp.event;
// event/TacheCreatedEvent.java

import com.example.backend_sirhgp.model.Tache;  // À adapter si votre classe s'appelle autrement
import com.example.backend_sirhgp.model.Employer;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class TacheCreatedEvent extends ApplicationEvent {

    private final Tache tache;
    private final Employer createur;
    private final Employer assigne;
    private final LocalDateTime createdAt;

    public TacheCreatedEvent(Object source, Tache tache, Employer createur, Employer assigne) {
        super(source);
        this.tache = tache;
        this.createur = createur;
        this.assigne = assigne;
        this.createdAt = LocalDateTime.now();
    }

    public Tache getTache() { return tache; }
    public Employer getCreateur() { return createur; }
    public Employer getAssigne() { return assigne; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
