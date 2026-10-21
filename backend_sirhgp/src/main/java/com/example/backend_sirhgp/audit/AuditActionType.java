package com.example.backend_sirhgp.audit;

/**
 * Enum des types d'actions tracées par le système d'audit.
 */
public enum AuditActionType {
    LOGIN("CONNEXION"),
    LOGOUT("DÉCONNEXION"),
    CREATE("CRÉATION"),
    READ("CONSULTATION"),
    UPDATE("MODIFICATION"),
    DELETE("SUPPRESSION"),
    TOGGLE_STATUS("CHANGEMENT DE STATUT"),
    CHANGE_PASSWORD("CHANGEMENT DE MOT DE PASSE"),
    DOWNLOAD("TÉLÉCHARGEMENT"),
    UPLOAD("TÉLÉVERSEMENT"),
    ASSIGN("ASSIGNATION"),
    DISPATCH("DISPATCH"),
    TRANSFER("TRANSFERT"),
    COMPLETE("COMPLÉTION"),
    LOGIN_FAILED("ÉCHEC DE CONNEXION"),
    // Refonte 5.9 — SUPER_ADMIN
    RESET_PASSWORD("RÉINITIALISATION MOT DE PASSE"),
    IMPERSONATE("IMPERSONATION"),
    STOP_IMPERSONATE("FIN D'IMPERSONATION"),
    OTHER("AUTRE");

    private final String label;

    AuditActionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}