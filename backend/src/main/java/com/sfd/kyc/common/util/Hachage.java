package com.sfd.kyc.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hachage {

    private Hachage() {
    }

    public static String sha256Hex(byte[] contenu) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    public static String sha256Hex(String texte) {
        return sha256Hex(texte.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean egalConstant(String attendu, String calcule) {
        if (attendu == null || calcule == null) {
            return false;
        }
        return MessageDigest.isEqual(
                attendu.getBytes(StandardCharsets.US_ASCII),
                calcule.getBytes(StandardCharsets.US_ASCII));
    }

    public static String hashPrecedentInitial() {
        return "0".repeat(64);
    }
}
