package com.sfd.kyc.common.audit;

public final class AuditAction {

    private AuditAction() {
    }

    public static final String LOGIN_SUCCES = "LOGIN_SUCCES";
    public static final String LOGIN_ECHEC = "LOGIN_ECHEC";
    public static final String DOSSIER_CREE = "DOSSIER_CREE";
    public static final String DOSSIER_MODIFIE = "DOSSIER_MODIFIE";
    public static final String DOCUMENT_DEPOSE = "DOCUMENT_DEPOSE";
    public static final String DOCUMENT_CONSULTE = "DOCUMENT_CONSULTE";
    public static final String DOSSIER_SOUMIS = "DOSSIER_SOUMIS";
    public static final String DOSSIER_ASSIGNE = "DOSSIER_ASSIGNE";
    public static final String SCORING_CALCULE = "SCORING_CALCULE";
    public static final String DOSSIER_VALIDE = "DOSSIER_VALIDE";
    public static final String DOSSIER_REJETE = "DOSSIER_REJETE";
    public static final String ACCES_REFUSE = "ACCES_REFUSE";
    public static final String UTILISATEUR_CREE = "UTILISATEUR_CREE";
    public static final String UTILISATEUR_MODIFIE = "UTILISATEUR_MODIFIE";
    public static final String CRITERE_CREE = "CRITERE_CREE";
    public static final String CRITERE_MODIFIE = "CRITERE_MODIFIE";
    public static final String CRITERE_SUPPRIME = "CRITERE_SUPPRIME";
    public static final String TOKEN_REUTILISE = "TOKEN_REUTILISE";
}
