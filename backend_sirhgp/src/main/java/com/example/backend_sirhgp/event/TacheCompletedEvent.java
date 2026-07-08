// event/TacheCompletedEvent.java
package com.example.backend_sirhgp.event;

import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.model.Employer;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class TacheCompletedEvent extends ApplicationEvent {

    private final Tache tache;
    private final Employer worker;
    private final LocalDateTime completedAt;

    public TacheCompletedEvent(Object source, Tache tache, Employer worker) {
        super(source);
        this.tache = tache;
        this.worker = worker;
        this.completedAt = LocalDateTime.now();
    }

    public Tache getTache() { return tache; }
    public Employer getWorker() { return worker; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}