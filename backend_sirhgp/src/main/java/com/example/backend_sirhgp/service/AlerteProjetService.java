package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Projet;
import com.example.backend_sirhgp.model.Tache;
import com.example.backend_sirhgp.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Alertes & risques projets (3.4) :
 * - job quotidien : tâches en retard et bloquées → créateur, assigné et chef de projet autorisés ;
 * - alerte immédiate quand une tâche passe à BLOQUEE (hook depuis TacheService).
 * Notifications via WebSocket (/queue/notifications), sans persistance ni email.
 * Déduplication quotidienne en mémoire, par tâche, type et destinataire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlerteProjetService {

    private final TacheRepository tacheRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TacheAccessService tacheAccessService;

    /** Déduplication quotidienne en mémoire (réinitialisée au redémarrage). */
    private final Map<String, LocalDate> alertesDuJour = new HashMap<>();
    private static final ZoneId ZONE = ZoneId.of("Africa/Douala");

    // ─── Job quotidien ───────────────────────────────────────────

    @Scheduled(cron = "0 0 6 * * *", zone = "Africa/Douala")
    @Transactional(readOnly = true)
    public void scannerAlertesQuotidiennes() {
        log.info("🔔 [3.4] Scan quotidien des alertes projets (retards / blocages)…");
        int nb = 0;
        for (Tache t : tacheRepository.findAll()) {
            nb += alerterSiEnRetard(t);
            nb += alerterSiBloquee(t);
        }
        log.info("🔔 [3.4] {} alerte(s) envoyée(s)", nb);
    }

    // ─── Hooks publics ───────────────────────────────────────────

    /** Appelé par TacheService.updateStatut quand une tâche passe à BLOQUEE. */
    public void alerterTacheBloquee(Tache tache) {
        alerter(tache, "🚫", "Tâche bloquée : " + tache.getTitre(),
                "La tâche « " + tache.getTitre() + " » a été marquée BLOQUEE. Action requise.");
    }

    // ─── Mécanique interne ───────────────────────────────────────

    private int alerterSiEnRetard(Tache t) {
        if (t.getDateEcheance() == null || t.getStatut() == Tache.StatutTache.TERMINEE) {
            return 0;
        }
        long jours = ChronoUnit.DAYS.between(t.getDateEcheance(), LocalDate.now(ZONE));
        if (jours <= 0) {
            return 0;
        }
        return alerter(t, "⚠️",
                "Tâche en retard de " + jours + " jour(s) : " + t.getTitre(),
                "La tâche « " + t.getTitre() + " », "
                        + (t.getAssignee() != null
                            ? "assignée à " + t.getAssignee().getName()
                            : "sans assigné")
                        + ", est en retard de " + jours + " jour(s)"
                        + " (échéance " + t.getDateEcheance() + ").");
    }

    private int alerterSiBloquee(Tache t) {
        if (t.getStatut() != Tache.StatutTache.BLOQUEE) {
            return 0;
        }
        return alerter(t, "🚫",
                "Tâche toujours bloquée : " + t.getTitre(),
                "La tâche « " + t.getTitre() + " » reste BLOQUEE — débloquez-la ou réévaluez le planning.");
    }

    /** Envoie aux personnes autorisées, sans doublon par destinataire, type et jour. */
    private synchronized int alerter(Tache t, String icone, String titre, String message) {
        LocalDate today = LocalDate.now(ZONE);
        alertesDuJour.entrySet().removeIf(entry -> !today.equals(entry.getValue()));

        Projet projet = t.getProjet();
        String projetNom = projet != null ? projet.getNom() : "Sans projet";
        String full = icone + " [" + projetNom + "] " + message;
        Map<Long, Employer> destinataires = new java.util.LinkedHashMap<>();
        ajouterDestinataire(destinataires, t.getCreateur());
        ajouterDestinataire(destinataires, t.getAssignee());
        if (projet != null) ajouterDestinataire(destinataires, projet.getChefProjet());

        int envoyees = 0;
        for (Employer dest : destinataires.values()) {
            if (!tacheAccessService.peutVoir(t, dest)) continue;
            String key = t.getId() + ":" + icone + ":" + dest.getId();
            if (today.equals(alertesDuJour.get(key))) continue;
            if (envoyer(dest, t.getId(), titre, full)) {
                alertesDuJour.put(key, today);
                envoyees++;
            }
        }
        return envoyees;
    }

    private void ajouterDestinataire(Map<Long, Employer> destinataires, Employer dest) {
        if (dest != null && dest.getId() != null && dest.getEmail() != null
                && !dest.getEmail().isBlank()) {
            destinataires.putIfAbsent(dest.getId(), dest);
        }
    }

    private boolean envoyer(Employer dest, Long taskId, String titre, String message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    dest.getEmail(),
                    "/queue/notifications",
                    new NotificationPayload(taskId, titre, message));
            log.info("🔔 Alerte projet → {} : {}", dest.getEmail(), titre);
            return true;
        } catch (Exception e) {
            log.warn("⚠️ Alerte non envoyée à {} : {}", dest.getEmail(), e.getMessage());
            return false;
        }
    }

    // Payload identique à NotificationService (même canal WebSocket)
    public static class NotificationPayload {
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
