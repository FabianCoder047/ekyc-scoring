package com.sfd.kyc.common.error;

public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String AUTHENTIFICATION_ECHOUEE = "AUTHENTIFICATION_ECHOUEE";
    public static final String TROP_DE_TENTATIVES = "TROP_DE_TENTATIVES";
    public static final String TOKEN_INVALIDE = "TOKEN_INVALIDE";
    public static final String REFRESH_TOKEN_INVALIDE = "REFRESH_TOKEN_INVALIDE";
    public static final String ACCES_REFUSE = "ACCES_REFUSE";
    public static final String RESSOURCE_INTROUVABLE = "RESSOURCE_INTROUVABLE";
    public static final String VALIDATION_ECHEC = "VALIDATION_ECHEC";
    public static final String TRANSITION_INVALIDE = "TRANSITION_INVALIDE";
    public static final String QUATRE_YEUX = "QUATRE_YEUX";
    public static final String DOCUMENT_ALTERE = "DOCUMENT_ALTERE";
    public static final String DOCUMENT_MANQUANT = "DOCUMENT_MANQUANT";
    public static final String TYPE_DOCUMENT_NON_SUPPORTE = "TYPE_DOCUMENT_NON_SUPPORTE";
    public static final String FICHIER_TROP_VOLUMINEUX = "FICHIER_TROP_VOLUMINEUX";
    public static final String FICHIER_ILLISIBLE = "FICHIER_ILLISIBLE";
    public static final String PIECE_EXPIREE = "PIECE_EXPIREE";
    public static final String SCORING_DONNEES_INCOMPLETES = "SCORING_DONNEES_INCOMPLETES";
    public static final String CRITERE_SCORING_MANQUANT = "CRITERE_SCORING_MANQUANT";
    public static final String CHEVAUCHEMENT_CRITERES = "CHEVAUCHEMENT_CRITERES";
    public static final String EMAIL_DEJA_UTILISE = "EMAIL_DEJA_UTILISE";
    public static final String COMPTE_DESACTIVE = "COMPTE_DESACTIVE";
    public static final String STOCKAGE_ERREUR = "STOCKAGE_ERREUR";
    public static final String REQUETE_INVALIDE = "REQUETE_INVALIDE";
    public static final String CONFLIT = "CONFLIT";
    public static final String ERREUR_INTERNE = "ERREUR_INTERNE";
}
