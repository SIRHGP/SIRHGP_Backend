package com.example.backend_sirhgp.listener;

import com.example.backend_sirhgp.event.FactureDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;  // ← AJOUTEZ CETTE LIGNE


@Slf4j
@Component
@RequiredArgsConstructor
public class FactureEventListener {

    @Async
    @EventListener
    public void handleFactureCreated(FactureDispatchedEvent event) {
        log.info("💰 Nouvelle facture envoyée: {} à {} (Montant: {}€)",
                event.getFacture().getNumeroFacture(),
                event.getClient().getEmail(),
                event.getFacture().getMontantTTC());

        // TODO: Actions à implémenter:
        // 1. Envoi d'email au client avec PDF attaché
        // 2. Notification in-app
        // 3. Si montant > seuil (ex: 10000€), alerte le manager
        // 4. Génération automatique du PDF si nécessaire
        // 5. Webhook vers système de facturation externe

        sendEmailNotification(event);
        createInAppNotification(event);
        checkHighAmount(event);
    }

    private void sendEmailNotification(FactureDispatchedEvent event) {
        log.debug("📧 Email à envoyer à {} pour la facture {}",
                event.getClient().getEmail(),
                event.getFacture().getNumeroFacture());

        // Template email suggéré:
        // Sujet: "Nouvelle facture disponible - {{facture.numeroFacture}}"
        // Corps:
        // "Bonjour {{client.name}},
        //
        //  Vous avez reçu une nouvelle facture de la part de {{emetteur.name}}.
        //
        //  Facture N°: {{facture.numeroFacture}}
        //  Montant TTC: {{facture.montantTTC}}€
        //  Date d'échéance: {{facture.dateEcheance}}
        //  Message: {{event.message}}
        //
        //  Consultez votre facture ici: /api/factures/{{facture.id}}/download"
    }

    private void createInAppNotification(FactureDispatchedEvent event) {
        log.debug("🔔 Notification in-app créée pour {} (facture: {})",
                event.getClient().getEmail(),
                event.getFacture().getNumeroFacture());

        // À implémenter avec une table Notification
        // Notification.builder()
        //     .employer(event.getClient())
        //     .type("FACTURE_RECUE")
        //     .message("Vous avez reçu une facture " + event.getFacture().getNumeroFacture())
        //     .lue(false)
        //     .build();
    }

    private void checkHighAmount(FactureDispatchedEvent event) {
        BigDecimal seuil = new BigDecimal("10000");
        if (event.getFacture().getMontantTTC().compareTo(seuil) > 0) {
            log.warn("⚠️ Facture de montant élevé ({}) envoyée à {}",
                    event.getFacture().getMontantTTC(),
                    event.getClient().getEmail());
            // TODO: Alerter le manager financier
        }
    }
}