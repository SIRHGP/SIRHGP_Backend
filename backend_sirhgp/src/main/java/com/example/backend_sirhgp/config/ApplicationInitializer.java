package com.example.backend_sirhgp.config;

import com.example.backend_sirhgp.model.*;
import com.example.backend_sirhgp.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationInitializer.class);

    private final ActionRepository actionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final DepartementRepository departementRepository;
    private final PosteRepository posteRepository;
    private final EmployerRepository employerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initialize-data:true}")
    private boolean initializeData;

    @Value("${app.migrate-legacy-secretary-role:false}")
    private boolean migrateLegacySecretaryRoleOnStartup;

    @Value("${app.default-users.superadmin.password:}")
    private String superAdminPassword;

    @Value("${app.default-users.admin.password:}")
    private String adminPassword;

    @Value("${app.default-users.manager.password:}")
    private String managerPassword;

    @Value("${app.default-users.staff.password:}")
    private String staffPassword;

    @Value("${app.default-users.stagiaire.password:}")
    private String stagiairePassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!initializeData) {
            log.info("Initialisation désactivée (app.initialize-data=false)");
            if (migrateLegacySecretaryRoleOnStartup) {
                log.info("Migration contrôlée du rôle SECRETAIRE_DIRECTION activée");
                migrateLegacySecretaryRole();
                normaliserSecretairesActives();
            }
            return;
        }

        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║     DÉMARRAGE DE L'INITIALISATION DE L'APPLICATION   ║");
        log.info("╚══════════════════════════════════════════════════════╝");

        try {
            initializeRoles();
            migrateLegacySecretaryRole();
            normaliserSecretairesActives();
            initializeDepartements();
            Map<String, Action> actions = initializeActions();
            Map<String, Permission> permissions = initializePermissions(actions);
            linkRolesToPermissions(permissions);
            initializePostes();
            initializeDefaultEmployees();

            log.info("╔══════════════════════════════════════════════════════╗");
            log.info("║          INITIALISATION TERMINÉE AVEC SUCCÈS         ║");
            log.info("╚══════════════════════════════════════════════════════╝");
        } catch (Exception e) {
            log.error("ERREUR CRITIQUE lors de l'initialisation : {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===================== 1. RÔLES =====================
    private void initializeRoles() {
        log.info("── [1/7] Initialisation des rôles ──");
        List<RoleConfig> configs = List.of(
                new RoleConfig("SUPER_ADMIN", "Accès total à toutes les fonctionnalités"),
                new RoleConfig("ADMIN", "Gestion des utilisateurs, paie et paramètres avancés"),
                new RoleConfig("MANAGER", "Gestion des équipes et validation des demandes"),
                new RoleConfig("LEAD", "Gestion des départements supervisés"),
                new RoleConfig("STAFF", "Accès aux fonctionnalités de base"),
                new RoleConfig("STAGIAIRE", "Accès limité en lecture seule"),
                new RoleConfig("SECRETAIRE", "Pointage des arrivées et accès transverse limité à sa succursale")
        );
        int created = 0;
        for (RoleConfig c : configs) {
            if (roleRepository.findByName(c.name()).isEmpty()) {
                roleRepository.save(Role.builder().name(c.name()).description(c.description()).build());
                created++;
                log.debug("✚ Rôle créé : {}", c.name());
            }
        }
        log.info("→ {} rôle(s) créé(s)", created);
    }

    private void migrateLegacySecretaryRole() {
        Optional<Role> legacyOpt = roleRepository.findByName("SECRETAIRE_DIRECTION");
        if (legacyOpt.isEmpty()) return;
        Role target = roleRepository.findByName("SECRETAIRE").orElseGet(() ->
                roleRepository.save(Role.builder().name("SECRETAIRE")
                        .description("Pointage des arrivées et accès transverse limité à sa succursale").build()));
        Role legacy = legacyOpt.get();
        if (legacy.getPermissions() != null) target.getPermissions().addAll(legacy.getPermissions());
        for (Employer employer : employerRepository.findByRoleId(legacy.getId())) {
            employer.setRole(target);
            employerRepository.save(employer);
        }
        roleRepository.save(target);
        roleRepository.delete(legacy);
    }

    private void normaliserSecretairesActives() {
        Role secretary = roleRepository.findByName("SECRETAIRE").orElse(null);
        Role staff = roleRepository.findByName("STAFF").orElse(null);
        if (secretary == null || staff == null) return;
        List<Employer> accounts = employerRepository.findByRoleId(secretary.getId()).stream()
                .filter(Employer::isActif)
                .sorted(Comparator.comparing(Employer::getDateCreation,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Employer::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Set<Long> branches = new HashSet<>();
        for (Employer account : accounts) {
            Long sucursalId = account.getSucursal() != null ? account.getSucursal().getId() : null;
            if (sucursalId == null || !branches.add(sucursalId)) {
                account.setRole(staff);
                employerRepository.save(account);
            }
        }
    }

    // ===================== 2. DÉPARTEMENTS =====================
    private void initializeDepartements() {
        log.info("── [2/7] Initialisation des départements ──");
        List<String> noms = List.of("Direction Générale", "Informatique & Systèmes", "Support & Opérations", "Auxiliaire");
        int created = 0;
        for (String nom : noms) {
            if (departementRepository.findByName(nom).isEmpty()) {
                departementRepository.save(Departement.builder().name(nom).build());
                created++;
                log.debug("✚ Département créé : {}", nom);
            }
        }
        log.info("→ {} département(s) créé(s)", created);
    }

    // ===================== 3. ACTIONS =====================
    private Map<String, Action> initializeActions() {
        log.info("── [3/7] Initialisation des actions ──");

        long existing = actionRepository.count();
        if (existing > 50) {
            log.info("→ {} actions déjà en base, chargement direct", existing);
            return loadAllActions();
        }

        Map<String, String> definitions = new LinkedHashMap<>();

        // Dashboard & Analytics
        definitions.put("view_dashboard", "Voir le tableau de bord principal");
        definitions.put("view_statistics", "Voir les statistiques globales");
        definitions.put("export_reports", "Exporter / générer des rapports");
        definitions.put("view_ai_analytics", "Consulter les analyses IA");
        definitions.put("run_ai_predictions", "Lancer des prédictions IA");

        // Documents
        definitions.put("upload_document", "Uploader un document");
        definitions.put("download_document", "Télécharger un document");
        definitions.put("export_document", "Exporter un document");
        definitions.put("import_document", "Importer un document");
        definitions.put("view_documents", "Consulter les documents");
        definitions.put("manage_documents", "Gérer les documents");
        definitions.put("share_document", "Partager un document");
        definitions.put("delete_document", "Supprimer un document");

        // Employés
        definitions.put("list_employees", "Lister les employés");
        definitions.put("view_employee_profile", "Voir le détail d'un employé");
        definitions.put("create_employee", "Créer un employé");
        definitions.put("update_employee", "Modifier un employé");
        definitions.put("archive_employee", "Archiver / désactiver un employé");
        definitions.put("evaluate_performance", "Évaluer les performances");
        definitions.put("plan_training", "Planifier une formation");
        definitions.put("upload_contract", "Uploader un contrat employé");
        definitions.put("validate_certificate", "Valider un certificat employé");
        definitions.put("assign_to_department", "Affecter un employé à un département");
        definitions.put("assign_to_project", "Affecter un employé à un projet");

        // Temps, Congés & Missions
        definitions.put("view_all_timesheets", "Voir toutes les feuilles de temps");
        definitions.put("submit_own_timesheet", "Soumettre sa propre feuille de temps");
        definitions.put("approve_timesheet", "Approuver une feuille de temps");
        definitions.put("request_leave", "Demander un congé");
        definitions.put("approve_leave", "Approuver un congé");
        definitions.put("view_own_leave_balance", "Consulter son solde de congés");
        definitions.put("view_all_leaves", "Voir tous les congés");
        definitions.put("manage_leave_types", "Gérer les types de congés");
        definitions.put("export_leave_reports", "Exporter les rapports de congés");
        definitions.put("create_mission", "Créer une mission");
        definitions.put("assign_mission", "Assigner une mission");
        definitions.put("validate_mission", "Valider une mission");

        // Paie
        definitions.put("view_own_payroll", "Voir sa propre fiche de paie");
        definitions.put("view_all_payrolls", "Voir toutes les fiches de paie");
        definitions.put("generate_payroll", "Générer la paie mensuelle");
        definitions.put("manage_payroll_settings", "Configurer les paramètres de paie");
        definitions.put("export_payroll_reports", "Exporter les rapports de paie");

        // Projets
        definitions.put("list_projects", "Lister les projets");
        definitions.put("view_project", "Voir le détail d'un projet");
        definitions.put("create_project", "Créer un projet");
        definitions.put("update_project", "Modifier un projet");
        definitions.put("delete_project", "Supprimer un projet");
        definitions.put("assign_team_member", "Assigner un membre à un projet");
        definitions.put("update_project_budget", "Modifier le budget d'un projet");
        definitions.put("view_project_reports", "Consulter les rapports de projet");

        // Tâches
        definitions.put("view_tasks", "Voir toutes les tâches");
        definitions.put("view_own_tasks", "Voir ses propres tâches");
        definitions.put("create_task", "Créer une tâche");
        definitions.put("update_task", "Modifier une tâche");
        definitions.put("delete_task", "Supprimer une tâche");
        definitions.put("assign_task", "Assigner une tâche");
        definitions.put("change_task_status", "Changer le statut d'une tâche");
        definitions.put("add_task_comment", "Commenter une tâche");
        definitions.put("view_task_history", "Voir l'historique d'une tâche");
        definitions.put("set_task_priority", "Définir la priorité d'une tâche");
        definitions.put("set_task_deadline", "Définir l'échéance d'une tâche");
        definitions.put("view_task_calendar", "Voir le calendrier des tâches");

        // Agenda / Calendrier
        definitions.put("view_calendar", "Voir son calendrier");
        definitions.put("view_team_calendar", "Voir le calendrier d'équipe");
        definitions.put("view_company_calendar", "Voir le calendrier de l'entreprise");
        definitions.put("create_event", "Créer un événement");
        definitions.put("update_event", "Modifier un événement");
        definitions.put("delete_event", "Supprimer un événement");
        definitions.put("invite_to_event", "Inviter des participants à un événement");
        definitions.put("set_event_reminder", "Définir un rappel d'événement");
        definitions.put("view_event_details", "Voir les détails d'un événement");
        definitions.put("export_calendar", "Exporter le calendrier");
        definitions.put("synchronize_calendar", "Synchroniser avec un calendrier externe");

        // Administration système
        definitions.put("manage_users", "Gérer les comptes utilisateurs");
        definitions.put("manage_roles", "Gérer les rôles");
        definitions.put("manage_permissions", "Gérer les permissions");
        definitions.put("view_audit_logs", "Consulter les logs d'audit");
        definitions.put("system_configuration", "Configurer le système");
        definitions.put("backup_database", "Sauvegarder la base de données");
        definitions.put("restore_database", "Restaurer la base de données");

        Map<String, Action> result = new HashMap<>();
        int created = 0;
        for (Map.Entry<String, String> entry : definitions.entrySet()) {
            Action action = actionRepository.findByName(entry.getKey())
                    .orElseGet(() -> {
                        Action a = actionRepository.save(
                                Action.builder()
                                        .name(entry.getKey())
                                        .description(entry.getValue())
                                        .build()
                        );
                        log.debug("✚ Action créée : {}", entry.getKey());
                        return a;
                    });
            result.put(entry.getKey(), action);
        }
        log.info("→ {} actions disponibles", result.size());
        return result;
    }

    private Map<String, Action> loadAllActions() {
        return actionRepository.findAll().stream()
                .collect(Collectors.toMap(Action::getName, a -> a));
    }

    // ===================== 4. PERMISSIONS =====================
    @Transactional
    protected Map<String, Permission> initializePermissions(Map<String, Action> all) {
        log.info("── [4/7] Initialisation des permissions ──");

        Map<String, Permission> perms = new LinkedHashMap<>();

        perms.put("gestion_employes", upsertPermission("gestion_employes",
                "Gestion complète des employés",
                Set.of("list_employees", "view_employee_profile", "create_employee",
                        "update_employee", "archive_employee", "evaluate_performance",
                        "plan_training", "upload_contract", "validate_certificate",
                        "assign_to_department", "assign_to_project"), all));

        perms.put("gestion_temps_conges", upsertPermission("gestion_temps_conges",
                "Gestion du temps, des congés et des missions",
                Set.of("view_all_timesheets", "submit_own_timesheet", "approve_timesheet",
                        "request_leave", "approve_leave", "view_own_leave_balance",
                        "view_all_leaves", "manage_leave_types", "export_leave_reports",
                        "create_mission", "assign_mission", "validate_mission"), all));

        perms.put("gestion_paie", upsertPermission("gestion_paie",
                "Gestion de la paie",
                Set.of("view_own_payroll", "view_all_payrolls", "generate_payroll",
                        "manage_payroll_settings", "export_payroll_reports"), all));

        perms.put("gestion_projets", upsertPermission("gestion_projets",
                "Gestion des projets",
                Set.of("list_projects", "view_project", "create_project", "update_project",
                        "delete_project", "assign_team_member", "update_project_budget",
                        "view_project_reports"), all));

        perms.put("gestion_taches", upsertPermission("gestion_taches",
                "Gestion des tâches",
                Set.of("view_tasks", "view_own_tasks", "create_task", "update_task",
                        "delete_task", "assign_task", "change_task_status", "add_task_comment",
                        "view_task_history", "set_task_priority", "set_task_deadline",
                        "view_task_calendar"), all));

        perms.put("gestion_agenda", upsertPermission("gestion_agenda",
                "Gestion des agendas et calendriers",
                Set.of("view_calendar", "view_team_calendar", "view_company_calendar",
                        "create_event", "update_event", "delete_event", "invite_to_event",
                        "set_event_reminder", "view_event_details", "export_calendar",
                        "synchronize_calendar"), all));

        perms.put("gestion_documents", upsertPermission("gestion_documents",
                "Gestion des documents",
                Set.of("upload_document", "download_document", "export_document",
                        "import_document", "view_documents", "manage_documents",
                        "share_document", "delete_document"), all));

        perms.put("acces_dashboard", upsertPermission("acces_dashboard",
                "Accès au tableau de bord et aux analytics IA",
                Set.of("view_dashboard", "view_statistics", "export_reports",
                        "view_ai_analytics", "run_ai_predictions"), all));

        perms.put("administration_systeme", upsertPermission("administration_systeme",
                "Administration système complète",
                Set.of("manage_users", "manage_roles", "manage_permissions", "view_audit_logs",
                        "system_configuration", "backup_database", "restore_database"), all));

        perms.put("gestion_equipe_limitee", upsertPermission("gestion_equipe_limitee",
                "Gestion limitée de l'équipe (Manager)",
                Set.of("list_employees", "view_employee_profile",
                        "approve_timesheet", "approve_leave",
                        "assign_task", "create_task", "view_tasks",
                        "view_all_timesheets", "view_all_leaves", "assign_mission",
                        "evaluate_performance"), all));

        perms.put("staff_basique", upsertPermission("staff_basique",
                "Droits basiques pour un employé (STAFF)",
                Set.of("submit_own_timesheet", "request_leave", "view_own_leave_balance",
                        "view_own_payroll", "view_own_tasks", "view_calendar",
                        "view_task_calendar", "create_event", "update_event", "delete_event",
                        "change_task_status", "add_task_comment",
                        "upload_document", "download_document", "export_document",
                        "import_document", "view_documents",
                        "view_event_details", "set_event_reminder"), all));

        perms.put("stagiaire_basique", upsertPermission("stagiaire_basique",
                "Droits restreints pour un stagiaire",
                Set.of("submit_own_timesheet", "view_own_tasks", "view_calendar",
                        "change_task_status", "add_task_comment",
                        "view_own_leave_balance", "download_document",
                        "view_documents", "view_event_details"), all));

        log.info("→ {} permissions initialisées", perms.size());
        return perms;
    }

    @Transactional
    protected Permission upsertPermission(String name, String description,
                                          Set<String> actionNames,
                                          Map<String, Action> allActions) {
        Permission perm = permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder().name(name).description(description).build()
                ));
        perm = permissionRepository.findByIdWithActions(perm.getId()).orElse(perm);
        Set<Action> toAdd = actionNames.stream()
                .map(allActions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        perm.getActions().addAll(toAdd);
        return permissionRepository.save(perm);
    }

    // ===================== 5. LIAISON RÔLES ↔ PERMISSIONS =====================
    @Transactional
    protected void linkRolesToPermissions(Map<String, Permission> perms) {
        log.info("── [5/7] Liaison rôles ↔ permissions ──");

        updateRolePermissions("SUPER_ADMIN", perms.keySet(), perms);
        updateRolePermissions("ADMIN", Set.of(
                "gestion_employes", "gestion_projets", "gestion_taches",
                "gestion_agenda", "acces_dashboard", "gestion_temps_conges",
                "gestion_paie", "gestion_documents", "administration_systeme"
        ), perms);
        updateRolePermissions("MANAGER", Set.of(
                "gestion_equipe_limitee", "gestion_taches", "gestion_agenda",
                "gestion_projets", "acces_dashboard", "gestion_documents",
                "gestion_temps_conges"
        ), perms);
        updateRolePermissions("LEAD", Set.of(
                "gestion_equipe_limitee", "gestion_taches", "gestion_agenda",
                "gestion_projets", "acces_dashboard", "gestion_temps_conges"
        ), perms);
        updateRolePermissions("STAFF", Set.of("staff_basique", "gestion_documents"), perms);
        updateRolePermissions("STAGIAIRE", Set.of("stagiaire_basique"), perms);
        updateRolePermissions("SECRETAIRE", Set.of(
                "gestion_equipe_limitee", "gestion_projets", "gestion_taches",
                "gestion_agenda", "gestion_documents", "gestion_temps_conges",
                "acces_dashboard"
        ), perms);

        log.info("→ Liaison rôles/permissions terminée");
    }

    private void updateRolePermissions(String roleName, Set<String> permNames, Map<String, Permission> perms) {
        Role role = roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() -> new IllegalStateException("Rôle introuvable : " + roleName));
        Set<Permission> newPerms = permNames.stream()
                .map(perms::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        role.getPermissions().clear();
        role.getPermissions().addAll(newPerms);
        roleRepository.save(role);
        log.info("→ {} : {} permission(s)", roleName, newPerms.size());
    }

    // ===================== 6. POSTES =====================
    private void initializePostes() {
        log.info("── [6/7] Initialisation des postes ──");

        Map<String, Departement> depts = departementRepository.findAll().stream()
                .collect(Collectors.toMap(Departement::getName, d -> d));
        Map<String, Role> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, r -> r));

        List<PosteConfig> configs = List.of(
                new PosteConfig("Directeur Général", "SUPER_ADMIN", "Direction Générale", "Direction générale..."),
                new PosteConfig("Responsable Paie", "ADMIN", "Direction Générale", "Administration de la paie..."),
                new PosteConfig("Comptable", "MANAGER", "Direction Générale", "Gestion comptable..."),
                new PosteConfig("Secrétaire", "SECRETAIRE", "Direction Générale", "Gestion administrative et pointage des arrivées..."),
                new PosteConfig("Chef de Département", "LEAD", "Informatique & Systèmes", "Encadrement du département..."),
                new PosteConfig("Directeur Technique", "ADMIN", "Informatique & Systèmes", "Responsable technique..."),
                new PosteConfig("Développeur", "MANAGER", "Informatique & Systèmes", "Développement logiciel..."),
                new PosteConfig("Technicien Support", "STAFF", "Support & Opérations", "Support technique..."),
                new PosteConfig("Cuisinier", "STAFF", "Auxiliaire", "Service de restauration"),
                new PosteConfig("Chauffeur", "STAFF", "Auxiliaire", "Service de transport"),
                new PosteConfig("Agent d'Entretien", "STAFF", "Auxiliaire", "Entretien des locaux"),
                new PosteConfig("Agent de Sécurité", "STAFF", "Auxiliaire", "Sécurité des locaux"),
                new PosteConfig("Stagiaire", "STAGIAIRE", "Auxiliaire", "Accès de stage limité")
        );

        int created = 0, skipped = 0, errors = 0;
        for (PosteConfig c : configs) {
            String nomNormalise = c.nom().trim();
            if (posteRepository.existsByName(nomNormalise)) {
                skipped++;
                continue;
            }
            Departement dept = depts.get(c.dept());
            Role role = roles.get(c.role());
            if (dept == null) {
                log.warn("⚠ Département '{}' introuvable pour le poste '{}'", c.dept(), nomNormalise);
                errors++;
                continue;
            }
            if (role == null) {
                log.warn("⚠ Rôle '{}' introuvable pour le poste '{}'", c.role(), nomNormalise);
                errors++;
                continue;
            }
            Poste poste = new Poste();
            poste.setName(nomNormalise);
            poste.setDescription(c.description());
            poste.setDepartement(dept);
            poste.setRole(role);
            posteRepository.save(poste);
            created++;
            log.debug("✚ Poste créé : {} [{}/{}]", nomNormalise, c.dept(), c.role());
        }
        log.info("→ {} poste(s) créé(s), {} déjà existant(s), {} erreur(s)", created, skipped, errors);
    }

    // ===================== 7. EMPLOYÉS PAR DÉFAUT =====================
    @Transactional
    protected void initializeDefaultEmployees() {
        log.info("── [7/7] Initialisation des employés par défaut ──");

        List<EmployeeConfig> configs = List.of(
                new EmployeeConfig("Abdel Razak Ndam", "superadmin@sirhgp.cm", superAdminPassword, "Directeur Général", "SUPER_ADMIN",
                        LocalDate.of(1968,5,4), "+237690208244", "Yaoundé", "Centre", "Melen"),
                new EmployeeConfig("Marie Claire Fokam", "admin@sirhgp.cm", adminPassword, "Directeur Technique", "ADMIN",
                        LocalDate.of(1980,3,15), "+237677123456", "Yaoundé", "Centre", "Bastos"),
                new EmployeeConfig("Jean Pierre Mbarga", "manager@sirhgp.cm", managerPassword, "Comptable", "MANAGER",
                        LocalDate.of(1985,7,22), "+237699234567", "Yaoundé", "Centre", "Nlongkak"),
                new EmployeeConfig("Aminatou Diallo", "staff@sirhgp.cm", staffPassword, "Développeur", "STAFF",
                        LocalDate.of(1992,11,8), "+237655345678", "Douala", "Littoral", "Akwa"),
                new EmployeeConfig("Kevin Talla", "stagiaire@sirhgp.cm", stagiairePassword, "Stagiaire", "STAGIAIRE",
                        LocalDate.of(2001,4,17), "+237674456789", "Yaoundé", "Centre", "Obili")
        );

        int created = 0, skipped = 0, updated = 0;
        for (EmployeeConfig c : configs) {
            String email = c.email().toLowerCase().trim();
            Poste poste = posteRepository.findByName(c.posteNom().trim())
                    .orElseThrow(() -> new IllegalStateException("Poste '" + c.posteNom() + "' introuvable"));
            Role role = roleRepository.findByName(c.roleNom())
                    .orElseThrow(() -> new IllegalStateException("Rôle '" + c.roleNom() + "' introuvable"));
            Optional<Employer> existing = employerRepository.findByEmail(email);
            if (existing.isPresent()) {
                Employer emp = existing.get();
                // Les comptes de démonstration ont un rôle et un poste fixes ;
                // resynchroniser évite les autorités supplémentaires d'un ancien poste.
                emp.setRole(role);
                emp.setPoste(poste);
                // Si un mot de passe est fourni via variable d'environnement, on le met à jour
                if (c.password() != null && !c.password().isBlank()) {
                    emp.setMotDePasse(passwordEncoder.encode(c.password()));
                    emp.setActif(true);
                    employerRepository.save(emp);
                    updated++;
                    log.debug("→ Employé '{}' mis à jour (mot de passe réinitialisé)", email);
                    continue;
                }
                employerRepository.save(emp);
                log.debug("→ Employé '{}' déjà existant (pas de mot de passe fourni, inchangé)", email);
                skipped++;
                continue;
            }
            if (c.password() == null || c.password().isBlank()) {
                String roleKey = switch (c.roleNom()) {
                    case "SUPER_ADMIN" -> "superadmin";
                    case "ADMIN" -> "admin";
                    case "MANAGER" -> "manager";
                    case "STAFF" -> "staff";
                    case "STAGIAIRE" -> "stagiaire";
                    default -> throw new IllegalStateException("Rôle de compte initial inconnu: " + c.roleNom());
                };
                log.warn("Compte de démonstration {} ignoré: définir app.default-users.{}.password pour le créer",
                        email, roleKey);
                skipped++;
                continue;
            }
            Employer emp = Employer.builder()
                    .name(c.name())
                    .email(email)
                    .motDePasse(passwordEncoder.encode(c.password()))
                    .poste(poste)
                    .role(role)
                    .actif(true)
                    .dateNaiss(c.dateNaiss())
                    .lieu(c.lieu())
                    .region(c.region())
                    .residence(c.residence())
                    .numeroTelephone(c.telephone())
                    .dateEmbauche(LocalDate.now())
                    .build();
            employerRepository.save(emp);
            created++;
            log.info("✚ Employé créé : {} ({}) — rôle: {}", c.name(), email, c.roleNom());
        }
        log.info("→ {} employé(s) créé(s), {} mis à jour, {} déjà existant(s)", created, updated, skipped);
    }

    // ===================== RECORDS INTERNES =====================
    private record RoleConfig(String name, String description) {}
    private record PosteConfig(String nom, String role, String dept, String description) {}
    private record EmployeeConfig(String name, String email, String password, String posteNom, String roleNom,
                                  LocalDate dateNaiss, String telephone, String lieu, String region, String residence) {}
}
