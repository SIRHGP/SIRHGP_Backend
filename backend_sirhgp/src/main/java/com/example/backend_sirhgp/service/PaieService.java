// ─── PaieService.java ────────────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.audit.AuditActionType;
import com.example.backend_sirhgp.dto.response.BulletinPaieResponseDTO;
import com.example.backend_sirhgp.dto.response.PaieHeuresDTO;
import com.example.backend_sirhgp.dto.response.PointageResponseDTO;
import com.example.backend_sirhgp.dto.request.PaieBulletinRequestDTO;
import com.example.backend_sirhgp.exception.ResourceNotFoundException;
import com.example.backend_sirhgp.mapper.BulletinPaieMapper;
import com.example.backend_sirhgp.model.BulletinPaie;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Pointage;
import com.example.backend_sirhgp.repository.BulletinPaieRepository;
import com.example.backend_sirhgp.repository.EmployerRepository;
import com.example.backend_sirhgp.repository.PointageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaieService {

    private final BulletinPaieRepository bulletinPaieRepository;
    private final EmployerRepository employerRepository;
    private final PointageRepository pointageRepository;
    private final BulletinPaieMapper bulletinPaieMapper;
    private final EntrepriseContextService entrepriseContextService;
    private final AuditLogService auditLogService;

    /** SMIG camerounais mensuel (entreprises < 100 salariés). */
    public static final BigDecimal SMIG_MENSUEL = BigDecimal.valueOf(42690);
    /** Majoration légale des heures supplémentaires : jour de semaine (06h-22h). */
    private static final BigDecimal HS_TAUX_JOUR = new BigDecimal("1.20");
    /** Majoration légale : nuit (22h-06h) et jours fériés. */
    private static final BigDecimal HS_TAUX_NUIT = new BigDecimal("1.50");

    @Value("${app.paie.cnps-salariale:0.044}")
    private double cnpsSalarialeRate;
    @Value("${app.paie.cnps-patronale:0.0505}")
    private double cnpsPatronaleRate;
    @Value("${app.paie.cnps-plafond:1500000}")
    private double cnpsPlafond;
    @Value("${app.paie.pvid:0.0175}")
    private double pvidRate;
    @Value("${app.paie.cfc:0.0011}")
    private double cfcRate;
    @Value("${app.paie.tpr:0.01}")
    private double tprRate;
    @Value("${app.paie.abattement-irpp:0.20}")
    private double abattementIrpp;
    @Value("${app.paie.heures-supp-taux:1.3}")
    private double heuresSuppTaux;

    // ─── Génération des bulletins pour une entreprise / période ──────────────

    @Transactional
    public List<BulletinPaieResponseDTO> genererBulletinsPourEntreprise(Integer mois, Integer annee) {
        Entreprise entreprise = entrepriseContextService.getEntrepriseOfCurrentUser();
        List<Employer> employes = entreprise != null
                ? employerRepository.findByEntrepriseId(entreprise.getId())
                : employerRepository.findAll();

        List<BulletinPaieResponseDTO> result = new ArrayList<>();
        for (Employer emp : employes) {
            if (!emp.isActif() || emp.getSalaireBase() == null) continue;
            if (bulletinPaieRepository.findByEmployeIdAndMoisAndAnnee(emp.getId(), mois, annee).isPresent()) continue;
            BulletinPaie b = calculerBulletin(emp, mois, annee, entreprise);
            b = bulletinPaieRepository.save(b);
            result.add(bulletinPaieMapper.toResponseDTO(b));
        }
        log.info("Génération paie {}-{} : {} bulletin(s)", mois, annee, result.size());
        return result;
    }

    /**
     * Calcul complet d'un bulletin camerounais.
     */
    private BulletinPaie calculerBulletin(Employer emp, Integer mois, Integer annee, Entreprise entreprise) {
        BigDecimal salaireBase = emp.getSalaireBase();
        BigDecimal heuresSupp = BigDecimal.ZERO;
        BigDecimal prime = BigDecimal.ZERO;
        BigDecimal indemnites = BigDecimal.ZERO;

        BigDecimal montantHeuresSupp = calculMontantHeuresSupp(heuresSupp, salaireBase);
        BigDecimal brut = salaireBase.add(montantHeuresSupp).add(prime).add(indemnites);

        BigDecimal assietteCnps = brut.min(BigDecimal.valueOf(cnpsPlafond));
        BigDecimal cnpsSal = assietteCnps.multiply(BigDecimal.valueOf(cnpsSalarialeRate));
        BigDecimal cnpsPat = assietteCnps.multiply(BigDecimal.valueOf(cnpsPatronaleRate));

        BigDecimal pvid = brut.multiply(BigDecimal.valueOf(pvidRate));
        BigDecimal cfc = brut.multiply(BigDecimal.valueOf(cfcRate));

        BigDecimal irpp = calculerIrpp(salaireBase);
        BigDecimal totalRetenues = cnpsSal.add(pvid).add(cfc).add(irpp);
        BigDecimal net = brut.subtract(totalRetenues);

        BigDecimal tpr = brut.multiply(BigDecimal.valueOf(tprRate));
        BigDecimal coutEmployeur = brut.add(cnpsPat).add(tpr);

        return BulletinPaie.builder()
                .employe(emp)
                .entreprise(entreprise != null ? entreprise : emp.getEntreprise())
                .mois(mois).annee(annee)
                .salaireBase(salaireBase)
                .heuresTravaillees(BigDecimal.ZERO)
                .heuresSupp(heuresSupp)
                .montantHeuresSupp(arrondi(montantHeuresSupp))
                .primes(arrondi(prime)).indemnites(arrondi(indemnites))
                .brut(arrondi(brut))
                .cnpsSalariale(arrondi(cnpsSal)).cnpsPatronale(arrondi(cnpsPat))
                .irpp(arrondi(irpp)).pvid(arrondi(pvid)).cfc(arrondi(cfc)).tpr(arrondi(tpr))
                .totalRetenues(arrondi(totalRetenues))
                .netAPayer(arrondi(net))
                .coutTotalEmployeur(arrondi(coutEmployeur))
                .build();
    }

    private BigDecimal calculMontantHeuresSupp(BigDecimal heuresSupp, BigDecimal salaireBase) {
        if (heuresSupp == null || salaireBase == null || salaireBase.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tauxHoraire = salaireBase.divide(BigDecimal.valueOf(173.33), 4, RoundingMode.HALF_UP);
        return heuresSupp.multiply(tauxHoraire).multiply(BigDecimal.valueOf(heuresSuppTaux));
    }

    /** Barème IRPP Cameroun progressif annuel, mensualisé. */
    private BigDecimal calculerIrpp(BigDecimal brutMensuel) {
        if (brutMensuel == null || brutMensuel.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal brutAnnuel = brutMensuel.multiply(BigDecimal.valueOf(12));
        BigDecimal revenuImposable = brutAnnuel.subtract(brutAnnuel.multiply(BigDecimal.valueOf(abattementIrpp)))
                .max(BigDecimal.ZERO);

        // Barème annuel par tranches (revenu imposable):
        // [0 - 750k] = 0% · [750k - 1,5M] = 10% · [1,5M - 3M] = 15% · [3M - 6M] = 25% · >6M = 35%
        double[] bornesBasses = {0, 750000, 1500000, 3000000, 6000000};
        double[] bornesHautes = {750000, 1500000, 3000000, 6000000, Double.MAX_VALUE};
        double[] taux        = {0.0, 0.10, 0.15, 0.25, 0.35};

        double revenu = revenuImposable.doubleValue();
        BigDecimal irpp = BigDecimal.ZERO;
        for (int i = 0; i < bornesBasses.length; i++) {
            double portion = Math.min(revenu, bornesHautes[i]) - bornesBasses[i];
            if (portion > 0) {
                irpp = irpp.add(BigDecimal.valueOf(portion).multiply(BigDecimal.valueOf(taux[i])));
            }
            if (revenu <= bornesHautes[i]) break;
        }
        return irpp.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal arrondi(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 5.1 — Synthèse des heures de tous les employés actifs de l'entreprise pour un mois
     * (association Timesheet ↔ Fiche de paie : jours ouvrables / présents / absents par user).
     * Pointages détaillés omis (payload allégé) — la fiche individuelle reste servie par
     * {@link #getHeuresEmploye(Long, Integer, Integer)}.
     */
    @Transactional(readOnly = true)
    public List<PaieHeuresDTO> getHeuresEntreprise(Integer mois, Integer annee) {
        Entreprise entreprise = entrepriseContextService.getEntrepriseOfCurrentUser();
        List<Employer> employes = entreprise != null
                ? employerRepository.findByEntrepriseId(entreprise.getId())
                : employerRepository.findAll();
        // Phase 2 - scoping fin : MANAGER/LEAD rattaches a une sucursal supervisent leur equipe uniquement.
        Long scopeSucursalId = entrepriseContextService.getScopeSucursalId();
        if (scopeSucursalId != null) {
            employes.removeIf(e -> e.getSucursal() == null || !scopeSucursalId.equals(e.getSucursal().getId()));
        }
        List<PaieHeuresDTO> result = new ArrayList<>();
        for (Employer emp : employes) {
            if (!emp.isActif()) continue;
            PaieHeuresDTO dto = getHeuresEmploye(emp.getId(), mois, annee);
            dto.setPointages(List.of());
            result.add(dto);
        }
        return result;
    }

    // ─── 5.1 : Résumé des heures pointées (pont Pointage → Paie) ─────────────

    /**
     * Résumé des heures d'un employé pour un mois : pointages + jours ouvrés/présents/manquants,
     * heures normales / supplémentaires (barème légal : +20 % jour 06h-22h, +50 % nuit 22h-06h),
     * retards cumulés et retenues suggérées.
     */
    @Transactional(readOnly = true)
    public PaieHeuresDTO getHeuresEmploye(Long employeId, Integer mois, Integer annee) {
        Employer emp = employerRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + employeId));
        verifierAccesEmploye(emp);

        YearMonth periode = YearMonth.of(annee, mois);
        LocalDate debut = periode.atDay(1);
        LocalDate fin = periode.atEndOfMonth();
        LocalDate aujourdHui = LocalDate.now();
        LocalDate limite = periode.atEndOfMonth().isAfter(aujourdHui) ? aujourdHui : periode.atEndOfMonth();

        List<Pointage> pointages = pointageRepository
                .findByEmployeIdAndDateBetweenOrderByDateDesc(employeId, debut, fin);

        Entreprise entreprise = emp.getEntreprise();
        LocalTime arriveeStd = entreprise != null && entreprise.getHeureArriveeStandard() != null
                ? entreprise.getHeureArriveeStandard() : LocalTime.of(8, 0);
        LocalTime departStd = entreprise != null && entreprise.getHeureDepartStandard() != null
                ? entreprise.getHeureDepartStandard() : LocalTime.of(17, 0);
        int minutesStd = (int) Duration.between(arriveeStd, departStd).toMinutes();
        if (minutesStd <= 0) minutesStd = 480;

        BigDecimal salaireBase = emp.getSalaireBase() != null ? emp.getSalaireBase() : BigDecimal.ZERO;
        BigDecimal tarifHoraire = BigDecimal.ZERO;
        if (salaireBase.signum() > 0) {
            tarifHoraire = salaireBase.divide(BigDecimal.valueOf(minutesStd), 6, RoundingMode.HALF_UP);
        }

        // Jours ouvrés (lun-ven) totaux et écoulés
        int joursOuvres = 0, joursEcoules = 0;
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                joursOuvres++;
                if (!d.isAfter(limite)) joursEcoules++;
            }
        }

        BigDecimal heuresNormales = BigDecimal.ZERO;
        BigDecimal heuresSupJour = BigDecimal.ZERO;
        BigDecimal heuresSupNuit = BigDecimal.ZERO;
        int retardsMinutes = 0;
        int joursPresents = 0;

        for (Pointage p : pointages) {
            if (p.getDepart() == null) continue; // journée incomplète : pas de durée
            joursPresents++;
            long duree = Math.max(0, Duration.between(p.getArrivee(), p.getDepart()).toMinutes());

            // Retard : arrivée après l'heure standard
            long retard = Duration.between(arriveeStd, p.getArrivee().toLocalTime()).toMinutes();
            if (retard > 0) retardsMinutes += (int) Math.min(retard, minutesStd);

            // Minutes de nuit travaillées (22h → 06h), pour répartir les heures sup
            long minutesNuit = minutesDeNuit(p.getArrivee(), p.getDepart());

            long supTotal = Math.max(0, duree - minutesStd);
            long supNuit = Math.min(supTotal, minutesNuit);
            long supJour = supTotal - supNuit;

            heuresNormales = heuresNormales.add(BigDecimal.valueOf(duree - supTotal));
            heuresSupJour = heuresSupJour.add(BigDecimal.valueOf(supJour));
            heuresSupNuit = heuresSupNuit.add(BigDecimal.valueOf(supNuit));
        }

        int joursManquants = Math.max(0, joursEcoules - joursPresents);
        BigDecimal retenuesAbsences = BigDecimal.ZERO;
        BigDecimal retenuesRetards = BigDecimal.ZERO;
        if (joursOuvres > 0 && salaireBase.signum() > 0) {
            // Taux journalier = salaire brut ÷ jours ouvrables du mois (cohérent avec la fiche)
            BigDecimal tarifJournalier = salaireBase.divide(BigDecimal.valueOf(joursOuvres), 2, RoundingMode.HALF_UP);
            retenuesAbsences = tarifJournalier.multiply(BigDecimal.valueOf(joursManquants));
        }
        if (salaireBase.signum() > 0 && retardsMinutes > 0) {
            retenuesRetards = tarifHoraire.multiply(BigDecimal.valueOf(retardsMinutes))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }



        List<String> avertissements = new ArrayList<>();
        if (salaireBase.signum() > 0 && salaireBase.compareTo(SMIG_MENSUEL) < 0) {
            avertissements.add("Salaire brut inférieur au SMIG camerounais (42 690 FCFA) — non conforme.");
        }
        if (joursManquants > 0) {
            avertissements.add(joursManquants + " jour(s) ouvré(s) sans pointage complet — retenue suggérée.");
        }
        if (retardsMinutes > 0) {
            avertissements.add("Retards cumulés : " + retardsMinutes + " min.");
        }

        List<PointageResponseDTO> pointageDtos = new ArrayList<>();
        for (Pointage p : pointages) {
            PointageResponseDTO dto = new PointageResponseDTO();
            dto.setId(p.getId());
            dto.setDate(p.getDate());
            dto.setArrivee(p.getArrivee());
            dto.setDepart(p.getDepart());
            dto.setCorrige(p.isCorrige());
            dto.setDureeMinutes(p.getDepart() != null
                    ? Math.max(0, Duration.between(p.getArrivee(), p.getDepart()).toMinutes()) : null);
            dto.setJourSemaine(p.getDate().getDayOfWeek().name());
            pointageDtos.add(dto);
        }

        return PaieHeuresDTO.builder()
                .employeId(emp.getId())
                .employeNom(emp.getName())
                .nui(emp.getNui())
                .rib(emp.getRib())
                .salaireBase(salaireBase)
                .mois(mois).annee(annee)
                .joursOuvres(joursOuvres)
                .joursEcoules(joursEcoules)
                .joursPresents(joursPresents)
                .joursManquants(joursManquants)
                .heuresNormales(heuresNormales)
                .heuresSupJour(heuresSupJour)
                .heuresSupNuit(heuresSupNuit)
                .retardsMinutes(retardsMinutes)
                .tarifHoraire(arrondi(tarifHoraire))
                .retenuesAbsences(arrondi(retenuesAbsences))
                .retenuesRetards(arrondi(retenuesRetards))
                .avertissements(avertissements)
                .pointages(pointageDtos)
                .build();
    }

    /** Minutes de la plage [arrivee, depart] situées entre 22h et 06h (nuit légale). */
    private long minutesDeNuit(LocalDateTime arrivee, LocalDateTime depart) {
        long total = 0;
        LocalDateTime curseur = arrivee;
        while (curseur.isBefore(depart)) {
            LocalTime t = curseur.toLocalTime();
            boolean nuit = t.isAfter(LocalTime.of(21, 59)) || t.isBefore(LocalTime.of(6, 0));
            if (nuit) total++;
            curseur = curseur.plusMinutes(1);
        }
        return total;
    }

    // ─── 5.1 : Génération d'un bulletin unitaire (depuis la fiche employé) ───

    @Transactional
    public BulletinPaieResponseDTO genererBulletinUnitaire(PaieBulletinRequestDTO req) {
        Employer emp = employerRepository.findById(req.getEmployeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé : " + req.getEmployeId()));
        if (emp.getSalaireBase() == null || emp.getSalaireBase().signum() <= 0) {
            throw new IllegalArgumentException("Cet employé n'a pas de salaire brut renseigné.");
        }
        if (bulletinPaieRepository.findByEmployeIdAndMoisAndAnnee(req.getEmployeId(), req.getMois(), req.getAnnee()).isPresent()) {
            throw new IllegalArgumentException("Un bulletin existe déjà pour cet employé sur cette période.");
        }

        PaieHeuresDTO heures = getHeuresEmploye(req.getEmployeId(), req.getMois(), req.getAnnee());

        BigDecimal salaireBase = emp.getSalaireBase();
        BigDecimal hSupJour = req.getHeuresSupJour() != null ? req.getHeuresSupJour() : heures.getHeuresSupJour();
        BigDecimal hSupNuit = req.getHeuresSupNuit() != null ? req.getHeuresSupNuit() : heures.getHeuresSupNuit();
        BigDecimal primes = req.getPrimes() != null ? req.getPrimes() : BigDecimal.ZERO;
        BigDecimal indemnites = req.getIndemnites() != null ? req.getIndemnites() : BigDecimal.ZERO;

        BigDecimal tarifHoraire = heures.getTarifHoraire() != null ? heures.getTarifHoraire() : BigDecimal.ZERO;
        BigDecimal montantHeuresSupp = hSupJour.multiply(tarifHoraire).multiply(HS_TAUX_JOUR)
                .add(hSupNuit.multiply(tarifHoraire).multiply(HS_TAUX_NUIT));

        BigDecimal retenuesDiverses = req.getRetenuesDiverses() != null ? req.getRetenuesDiverses()
                : heures.getRetenuesAbsences().add(heures.getRetenuesRetards());

        BulletinPaie b = BulletinPaie.builder()
                .employe(emp)
                .entreprise(emp.getEntreprise())
                .mois(req.getMois()).annee(req.getAnnee())
                .salaireBase(salaireBase)
                .heuresTravaillees(heures.getHeuresNormales().divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP))
                .heuresSupp(hSupJour.add(hSupNuit))
                .montantHeuresSupp(arrondi(montantHeuresSupp))
                .primes(arrondi(primes))
                .indemnites(arrondi(indemnites))
                .retenuesDiverses(arrondi(retenuesDiverses))
                .build();

        // Cotisations (mêmes règles que la génération en masse)
        BigDecimal brut = salaireBase.add(montantHeuresSupp).add(primes).add(indemnites);
        BigDecimal assietteCnps = brut.min(BigDecimal.valueOf(cnpsPlafond));
        BigDecimal cnpsSal = assietteCnps.multiply(BigDecimal.valueOf(cnpsSalarialeRate));
        BigDecimal cnpsPat = assietteCnps.multiply(BigDecimal.valueOf(cnpsPatronaleRate));
        BigDecimal pvid = brut.multiply(BigDecimal.valueOf(pvidRate));
        BigDecimal cfc = brut.multiply(BigDecimal.valueOf(cfcRate));
        BigDecimal irpp = calculerIrpp(salaireBase);
        BigDecimal totalRetenues = cnpsSal.add(pvid).add(cfc).add(irpp).add(retenuesDiverses);
        BigDecimal net = brut.subtract(totalRetenues);
        BigDecimal tpr = brut.multiply(BigDecimal.valueOf(tprRate));

        b.setBrut(arrondi(brut));
        b.setCnpsSalariale(arrondi(cnpsSal));
        b.setCnpsPatronale(arrondi(cnpsPat));
        b.setIrpp(arrondi(irpp));
        b.setPvid(arrondi(pvid));
        b.setCfc(arrondi(cfc));
        b.setTpr(arrondi(tpr));
        b.setTotalRetenues(arrondi(totalRetenues));
        b.setNetAPayer(arrondi(net));
        b.setCoutTotalEmployeur(arrondi(brut.add(cnpsPat).add(tpr)));

        b = bulletinPaieRepository.save(b);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.CREATE,
                "Bulletin unitaire généré : employé " + emp.getId() + " période "
                        + req.getMois() + "/" + req.getAnnee(),
                "BulletinPaie", b.getId(), emp.getName());
        return bulletinPaieMapper.toResponseDTO(b);
    }

    /**
     * Modification d'un bulletin : primes / indemnités / retenues diverses mises à jour,
     * tous les montants dérivés (brut, cotisations, IRPP, retenues, net, coût employeur)
     * sont recalculés avec les mêmes règles que la génération.
     */
    @Transactional
    public BulletinPaieResponseDTO updateBulletin(Long id, PaieBulletinRequestDTO req) {
        BulletinPaie b = bulletinPaieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin non trouvé : " + id));
        Employer emp = b.getEmploye();

        BigDecimal primes = req.getPrimes() != null ? req.getPrimes() : nz(b.getPrimes());
        BigDecimal indemnites = req.getIndemnites() != null ? req.getIndemnites() : nz(b.getIndemnites());
        BigDecimal retenuesDiverses = req.getRetenuesDiverses() != null ? req.getRetenuesDiverses() : nz(b.getRetenuesDiverses());

        BigDecimal salaireBase = nz(b.getSalaireBase());
        BigDecimal montantHeuresSupp = nz(b.getMontantHeuresSupp());

        BigDecimal brut = salaireBase.add(montantHeuresSupp).add(primes).add(indemnites);
        BigDecimal assietteCnps = brut.min(BigDecimal.valueOf(cnpsPlafond));
        BigDecimal cnpsSal = assietteCnps.multiply(BigDecimal.valueOf(cnpsSalarialeRate));
        BigDecimal cnpsPat = assietteCnps.multiply(BigDecimal.valueOf(cnpsPatronaleRate));
        BigDecimal pvid = brut.multiply(BigDecimal.valueOf(pvidRate));
        BigDecimal cfc = brut.multiply(BigDecimal.valueOf(cfcRate));
        BigDecimal irpp = calculerIrpp(salaireBase);
        BigDecimal totalRetenues = cnpsSal.add(pvid).add(cfc).add(irpp).add(retenuesDiverses);
        BigDecimal net = brut.subtract(totalRetenues);
        BigDecimal tpr = brut.multiply(BigDecimal.valueOf(tprRate));

        b.setPrimes(arrondi(primes));
        b.setIndemnites(arrondi(indemnites));
        b.setRetenuesDiverses(arrondi(retenuesDiverses));
        b.setBrut(arrondi(brut));
        b.setCnpsSalariale(arrondi(cnpsSal));
        b.setCnpsPatronale(arrondi(cnpsPat));
        b.setIrpp(arrondi(irpp));
        b.setPvid(arrondi(pvid));
        b.setCfc(arrondi(cfc));
        b.setTpr(arrondi(tpr));
        b.setTotalRetenues(arrondi(totalRetenues));
        b.setNetAPayer(arrondi(net));
        b.setCoutTotalEmployeur(arrondi(brut.add(cnpsPat).add(tpr)));

        b = bulletinPaieRepository.save(b);
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.UPDATE,
                "Bulletin modifié : employé " + emp.getId() + " période " + b.getMois() + "/" + b.getAnnee(),
                "BulletinPaie", b.getId(), emp.getName());
        return bulletinPaieMapper.toResponseDTO(b);
    }

    /** Suppression d'un bulletin de paie. */
    @Transactional
    public void deleteBulletin(Long id) {
        BulletinPaie b = bulletinPaieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin non trouvé : " + id));
        auditLogService.record(entrepriseContextService.getCurrentEmployer(), AuditActionType.DELETE,
                "Bulletin supprimé : employé " + b.getEmploye().getId() + " période " + b.getMois() + "/" + b.getAnnee(),
                "BulletinPaie", b.getId(), b.getEmploye().getName());
        bulletinPaieRepository.delete(b);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public Page<BulletinPaieResponseDTO> consulter(Pageable pageable, Long employeId, Integer mois, Integer annee) {
        String role = entrepriseContextService.currentRole();
        boolean gestion = "ADMIN".equals(role) || "MANAGER".equals(role) || "LEAD".equals(role);

        // Un STAFF / STAGIAIRE ne voit que SES bulletins (lecture seule) — on ignore
        // tout employeId/filtre fourni par le client pour éviter de lire ceux d'autrui.
        if (!gestion) {
            Long moi = entrepriseContextService.getCurrentEmployer().getId();
            employeId = moi;
            mois = null;
            annee = null;
        }

        Long scope = entrepriseContextService.getScopeEntrepriseId();
        Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
        List<BulletinPaieResponseDTO> list;
        if (employeId != null) {
            list = bulletinPaieRepository
                    .findByEmployeIdOrderByAnneeDescMoisDesc(employeId)
                    .stream().map(bulletinPaieMapper::toResponseDTO).toList();
        } else if (scope != null && scopeSucursal != null) {
            // MANAGER / LEAD ancré à une succursale : que les bulletins de SON agence.
            list = (mois != null && annee != null)
                    ? bulletinPaieRepository.findByEntrepriseIdAndSucursalIdAndPeriode(scope, scopeSucursal, mois, annee).stream()
                            .map(bulletinPaieMapper::toResponseDTO).toList()
                    : bulletinPaieRepository.findByEntrepriseIdAndSucursalId(scope, scopeSucursal).stream()
                            .map(bulletinPaieMapper::toResponseDTO).toList();
        } else if (scope != null) {
            list = (mois != null && annee != null)
                    ? bulletinPaieRepository.findByEntrepriseIdAndPeriode(scope, mois, annee).stream()
                            .map(bulletinPaieMapper::toResponseDTO).toList()
                    : bulletinPaieRepository.findByEntrepriseId(scope).stream()
                            .map(bulletinPaieMapper::toResponseDTO).toList();
        } else {
            list = bulletinPaieRepository.findAll().stream()
                    .map(bulletinPaieMapper::toResponseDTO).toList();
        }
        return springPage(list, pageable);
    }

    @Transactional(readOnly = true)
    public BulletinPaie getEntity(Long id) {
        return bulletinPaieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin non trouvé : " + id));
    }

    /** Charge le bulletin avec son employé (pour la génération PDF hors session Hibernate). */
    @Transactional(readOnly = true)
    public BulletinPaie getEntityForPdf(Long id) {
        BulletinPaie b = bulletinPaieRepository.findByIdWithEmploye(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulletin non trouvé : " + id));
        if (b.getEmploye() != null) verifierAccesEmploye(b.getEmploye());
        return b;
    }

    /**
     * Contrôle d'accès à la fiche de paie / heures d'un employé (RG-M6-07) :
     * - STAFF / STAGIAIRE : uniquement SES propres données ;
     * - MANAGER / LEAD : périmètre succursale (même règle que getScopeSucursalId() ;
     *   un LEAD est aussi borné à ses départements supervisés) ;
     * - ADMIN / SUPER_ADMIN : tout leur périmètre entreprise.
     */
    private void verifierAccesEmploye(Employer cible) {
        String role = entrepriseContextService.currentRole();
        boolean gestion = "ADMIN".equals(role) || "MANAGER".equals(role) || "LEAD".equals(role);
        if (gestion) {
            Long scopeSucursal = entrepriseContextService.getScopeSucursalId();
            if (scopeSucursal != null) {
                Long sucCible = cible.getSucursal() != null ? cible.getSucursal().getId() : null;
                if (!scopeSucursal.equals(sucCible)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Cet employé n'appartient pas à votre périmètre (succursale).");
                }
            }
            return;
        }
        // Non-gestion (STAFF / STAGIAIRE) : soi-même uniquement.
        Employer moi = entrepriseContextService.getCurrentEmployer();
        if (moi == null || !moi.getId().equals(cible.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez consulter que vos propres informations de paie.");
        }
    }

    private <T> Page<T> springPage(List<T> list, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), list.size());
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
    }
}