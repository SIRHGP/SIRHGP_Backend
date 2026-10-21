// ─── PointageService.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.response.PointageResponseDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Pointage;
import com.example.backend_sirhgp.repository.PointageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Pointage (badgeuse) — 4.1.
 * Un clic à l'arrivée, un clic au départ ; durée calculée automatiquement.
 * 5 jours sur 7 (lundi→vendredi). Un seul pointage par jour et par employé.
 * L'employé pointe uniquement pour lui-même (utilisateur forcé côté serveur).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointageService {

    private final PointageRepository pointageRepository;
    private final EntrepriseContextService entrepriseContextService;

    private Employer requireCurrentUser() {
        Employer current = entrepriseContextService.getCurrentEmployer();
        if (current == null) {
            throw new ResourceNotFoundException("Utilisateur non trouvé");
        }
        return current;
    }

    private void checkJourOuvre(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException(
                    "Jour non ouvré — la pointeuse fonctionne du lundi au vendredi (5j/7)");
        }
    }

    @Transactional
    public PointageResponseDTO clockIn() {
        Employer current = requireCurrentUser();
        LocalDate today = LocalDate.now();
        checkJourOuvre(today);

        pointageRepository.findByEmployeIdAndDate(current.getId(), today).ifPresent(p -> {
            throw new IllegalArgumentException(p.getDepart() == null
                    ? "Vous avez déjà pointé votre arrivée aujourd'hui — pointez votre départ"
                    : "La journée du jour est déjà complète");
        });

        Pointage p = Pointage.builder()
                .date(today)
                .arrivee(LocalDateTime.now())
                .employe(current)
                .entreprise(current.getEntreprise())
                .build();
        p = pointageRepository.save(p);
        log.info("Pointage arrivée : {} à {}", current.getEmail(), p.getArrivee());
        return toDTO(p);
    }

    @Transactional
    public PointageResponseDTO clockOut() {
        Employer current = requireCurrentUser();
        LocalDate today = LocalDate.now();

        Pointage p = pointageRepository.findByEmployeIdAndDate(current.getId(), today)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun pointage d'arrivée aujourd'hui — pointez d'abord votre arrivée"));
        if (p.getDepart() != null) {
            throw new IllegalArgumentException("Votre départ a déjà été enregistré aujourd'hui");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(p.getArrivee())) {
            throw new IllegalArgumentException("L'heure de départ doit être postérieure à l'arrivée");
        }
        p.setDepart(now);
        p = pointageRepository.save(p);
        log.info("Pointage départ : {} à {} ({} min)", current.getEmail(), now, dureeMinutes(p));
        return toDTO(p);
    }

    /** Rattrapage d'oubli : saisie manuelle de l'heure de départ (marqué « corrigé »). */
    @Transactional
    public PointageResponseDTO corrigerDepart(Long id, LocalTime heureDepart) {
        Employer current = requireCurrentUser();
        Pointage p = pointageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pointage non trouvé : " + id));
        if (!current.getId().equals(p.getEmploye().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez corriger que vos propres pointages");
        }

        LocalDateTime depart = p.getDate().atTime(heureDepart);
        if (!depart.isAfter(p.getArrivee())) {
            throw new IllegalArgumentException(
                    "L'heure de départ doit être postérieure à l'heure d'arrivée ("
                            + p.getArrivee().toLocalTime() + ")");
        }
        if (depart.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Impossible de saisir une heure de départ dans le futur");
        }

        p.setDepart(depart);
        p.setCorrige(true);
        p = pointageRepository.save(p);
        log.info("Départ corrigé : {} ({})", current.getEmail(), depart);
        return toDTO(p);
    }

    @Transactional(readOnly = true)
    public PointageResponseDTO aujourdhui() {
        Employer current = requireCurrentUser();
        return pointageRepository.findByEmployeIdAndDate(current.getId(), LocalDate.now())
                .map(this::toDTO).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PointageResponseDTO> periode(LocalDate debut, LocalDate fin) {
        Employer current = requireCurrentUser();
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin précède la date de début");
        }
        return pointageRepository.findByEmployeIdAndDateBetweenOrderByDateDesc(
                        current.getId(), debut, fin).stream()
                .map(this::toDTO)
                .toList();
    }

    // ─── Utilitaires ─────────────────────────────────────────
    private Long dureeMinutes(Pointage p) {
        if (p.getDepart() == null) return null;
        return java.time.Duration.between(p.getArrivee(), p.getDepart()).toMinutes();
    }

    private PointageResponseDTO toDTO(Pointage p) {
        PointageResponseDTO dto = new PointageResponseDTO();
        dto.setId(p.getId());
        dto.setDate(p.getDate());
        dto.setArrivee(p.getArrivee());
        dto.setDepart(p.getDepart());
        dto.setCorrige(p.isCorrige());
        dto.setDureeMinutes(dureeMinutes(p));
        dto.setJourSemaine(p.getDate().getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH));

        // Règles pointage (4.2) : calculées depuis la config de l'entreprise de l'employé
        if (p.getEmploye() != null && p.getEmploye().getEntreprise() != null) {
            Entreprise ent = p.getEmploye().getEntreprise();
            if (ent.getHeureArriveeStandard() != null) {
                dto.setRetarde(p.getArrivee().toLocalTime().isAfter(ent.getHeureArriveeStandard()));
            }
            if (ent.getHeureDepartStandard() != null && p.getDepart() != null) {
                dto.setDepartAnticipe(p.getDepart().toLocalTime().isBefore(ent.getHeureDepartStandard()));
            }
            if (ent.getHeureArriveeMinimale() != null) {
                dto.setArriveeAnormale(p.getArrivee().toLocalTime().isBefore(ent.getHeureArriveeMinimale()));
            }
        }
        return dto;
    }
}