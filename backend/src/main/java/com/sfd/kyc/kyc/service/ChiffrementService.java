package com.sfd.kyc.kyc.service;

import com.sfd.kyc.common.config.AppProperties;
import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Chiffrement AES-256-GCM des documents (SEC-06). */
@Service
public class ChiffrementService {

    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec cle;
    private final SecureRandom random = new SecureRandom();

    public ChiffrementService(AppProperties proprietes) {
        byte[] octets = Base64.getDecoder().decode(proprietes.chiffrement().cle());
        if (octets.length != 32) {
            throw new IllegalStateException(
                    "KYC_ENCRYPTION_KEY doit être une clé AES-256 encodée en base64 (32 octets).");
        }
        this.cle = new SecretKeySpec(octets, "AES");
    }

    public Chiffre chiffrer(byte[] clair) {
        try {
            byte[] iv = new byte[TAILLE_IV];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG, iv));
            cipher.updateAAD("sfd-kyc-document-v1".getBytes(StandardCharsets.US_ASCII));
            return new Chiffre(cipher.doFinal(clair), iv);
        } catch (GeneralSecurityException e) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STOCKAGE_ERREUR,
                    "Le chiffrement du document a échoué.");
        }
    }

    public byte[] dechiffrer(byte[] chiffre, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG, iv));
            cipher.updateAAD("sfd-kyc-document-v1".getBytes(StandardCharsets.US_ASCII));
            return cipher.doFinal(chiffre);
        } catch (GeneralSecurityException e) {
            // Ne jamais distinguer clé erronée, tag invalide ou fichier tronqué.
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCodes.DOCUMENT_ALTERE,
                    "Le document est corrompu ou a été altéré.");
        }
    }

    public record Chiffre(byte[] contenu, byte[] iv) {
    }
}
