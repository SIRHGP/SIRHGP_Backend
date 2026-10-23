package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.dto.response.SecretariatEmployeDTO;
import com.example.backend_sirhgp.model.DemandePermission;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Pointage;
import com.example.backend_sirhgp.repository.DemandePermissionRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.PointageRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SecretariatService {
    private final EntrepriseContextService context;
    private final EmployerRepository employerRepository;
    private final PointageRepository pointageRepository;
    private final DemandePermissionRepository permissionRepository;
    private final PointageService pointageService;

    @Transactional(readOnly = true)
    public List<SecretariatEmployeDTO> employesDuJour() {
        Employer current = requireSecretary();
        LocalDate today = LocalDate.now();
        return employerRepository.findBySucursalIdAndActifTrueOrderByNameAsc(current.getSucursal().getId()).stream()
                .filter(e -> e.getEntreprise() != null && current.getEntreprise().getId().equals(e.getEntreprise().getId()))
                .map(e -> {
                    SecretariatEmployeDTO dto = new SecretariatEmployeDTO();
                    dto.setId(e.getId());
                    dto.setName(e.getName());
                    dto.setEmail(e.getEmail());
                    dto.setPosteName(e.getPoste() != null ? e.getPoste().getName() : null);
                    pointageRepository.findByEmployeIdAndDate(e.getId(), today).ifPresent(p -> {
                        dto.setHeureArrivee(p.getArrivee());
                        dto.setHeureDepart(p.getDepart());
                    });
                    return dto;
                }).toList();
    }

    @Transactional
    public void enregistrerArrivee(Long employeId) {
        requireSecretary();
        pointageService.clockInForEmployee(employeId);
    }

    @Transactional(readOnly = true)
    public byte[] rapportMensuel(String mois) {
        Employer secretary = requireSecretary();
        YearMonth period;
        try { period = YearMonth.parse(mois); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Le mois doit être au format AAAA-MM."); }
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();
        if (period.equals(YearMonth.now())) end = LocalDate.now();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4.rotate(), 30, 30, 34, 34);
        try {
            PdfWriter.getInstance(pdf, output);
            pdf.open();
            pdf.add(new Paragraph("Rapport mensuel des présences", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            pdf.add(new Paragraph("Succursale : " + secretary.getSucursal().getName() + " — " + period, FontFactory.getFont(FontFactory.HELVETICA, 11)));
            pdf.add(Chunk.NEWLINE);
            PdfPTable table = new PdfPTable(new float[]{4, 1.2f, 1.2f, 1.8f});
            table.setWidthPercentage(100);
            for (String title : List.of("Employé", "Présents", "Absents", "Heures calculées")) {
                table.addCell(new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            }
            for (Employer employee : employerRepository.findBySucursalIdAndActifTrueOrderByNameAsc(secretary.getSucursal().getId())) {
                if (employee.getEntreprise() == null || !secretary.getEntreprise().getId().equals(employee.getEntreprise().getId())) continue;
                List<Pointage> pointages = pointageRepository.findByEmployeIdAndDateBetweenOrderByDateAsc(employee.getId(), start, end);
                Set<LocalDate> presents = new HashSet<>();
                long minutes = 0;
                for (Pointage p : pointages) {
                    presents.add(p.getDate());
                    if (p.getDepart() != null && p.getArrivee() != null && p.getDepart().isAfter(p.getArrivee())) {
                        minutes += ChronoUnit.MINUTES.between(p.getArrivee(), p.getDepart());
                    }
                }
                Set<LocalDate> absencesApprouvees = new HashSet<>();
                for (DemandePermission permission : permissionRepository.findAbsencesApprouvees(employee.getId(), start, end)) {
                    for (LocalDate d = permission.getDateDebut().isBefore(start) ? start : permission.getDateDebut();
                         !d.isAfter(permission.getDateFin().isAfter(end) ? end : permission.getDateFin()); d = d.plusDays(1)) {
                        absencesApprouvees.add(d);
                    }
                }
                int workDays = 0, absences = 0;
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                    workDays++;
                    if (!presents.contains(d) && !absencesApprouvees.contains(d)) absences++;
                }
                table.addCell(employee.getName() + " (" + employee.getEmail() + ")");
                table.addCell(Integer.toString(presents.size()));
                table.addCell(Integer.toString(absences));
                table.addCell(String.format(Locale.ROOT, "%d h %02d min", minutes / 60, minutes % 60));
            }
            pdf.add(table);
            pdf.add(Chunk.NEWLINE);
            pdf.add(new Paragraph("Période comptabilisée jusqu'au " + end + ". Jours attendus : lundi à vendredi; absences approuvées exclues.", FontFactory.getFont(FontFactory.HELVETICA, 9)));
            pdf.close();
            return output.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le rapport PDF.", e);
        }
    }

    private Employer requireSecretary() {
        Employer current = context.getCurrentEmployer();
        if (current == null || current.getRole() == null || !"SECRETAIRE".equals(current.getRole().getName())) {
            throw new AccessDeniedException("Accès réservé au rôle SECRETAIRE.");
        }
        if (current.getEntreprise() == null || current.getSucursal() == null) {
            throw new AccessDeniedException("Votre compte doit être associé à une entreprise et une succursale.");
        }
        return current;
    }
}
