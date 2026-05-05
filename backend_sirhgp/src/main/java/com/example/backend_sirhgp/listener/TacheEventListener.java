// listener/TacheEventListener.java
package com.example.backend_sirhgp.listener;

import com.example.backend_sirhgp.event.TacheCreatedEvent;
import com.example.backend_sirhgp.event.TacheCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TacheEventListener {

    @Async
    @EventListener
    public void handleTacheCreated(TacheCreatedEvent event) {
        log.info("📋 Nouvelle tâche créée: {} assignée à {} par {}",
                event.getTache().getTitre(),
                event.getAssigne().getEmail(),
                event.getCreateur().getEmail());

        // TODO: Appeler vos services existants
        // 1. notificationService.envoyerNotificationAssignation(event.getAssigne(), event.getTache())
        // 2. agendaService.ajouterEvenement(event.getAssigne(), event.getTache())
        // 3. Si tâche payante -> factureService.creerFacture(event.getTache())
    }

    @Async
    @EventListener
    public void handleTacheCompleted(TacheCompletedEvent event) {
        log.info("✅ Tâche terminée: {} par {}",
                event.getTache().getTitre(),
                event.getWorker().getEmail());

        // TODO: Actions à déclencher
        // 1. notificationService.notifierCreateur(event.getTache())
        // 2. Si facture associée -> factureService.marquerPayee(event.getTache())
    }
}