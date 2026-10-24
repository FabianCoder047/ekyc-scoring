package com.sfd.kyc.common.util;

/** Masquage des données personnelles (SEC-10). */
public final class Masquage {

    private Masquage() {
    }

    /**
     * Conserve les deux premiers caractères et les quatre derniers :
     * {@code TG1234567891234} devient {@code TG****1234}.
     */
    public static String numeroPiece(String numero) {
        if (numero == null || numero.isBlank()) {
            return "****";
        }
        String valeur = numero.trim();
        if (valeur.length() <= 6) {
            return "****";
        }
        return valeur.substring(0, 2) + "****" + valeur.substring(valeur.length() - 4);
    }
}
