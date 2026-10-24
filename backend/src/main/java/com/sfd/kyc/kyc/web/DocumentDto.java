package com.sfd.kyc.kyc.web;

import com.sfd.kyc.kyc.domain.DocumentKyc;
import java.time.Instant;

public record DocumentDto(
        Long id,
        String type,
        String nomOriginal,
        String mimeType,
        long tailleOctets,
        Instant deposeLe,
        Long remplacePar) {

    public static DocumentDto de(DocumentKyc document) {
        return new DocumentDto(
                document.getId(),
                document.getType().name(),
                document.getNomOriginal(),
                document.getMimeType(),
                document.getTailleOctets(),
                document.getDeposeLe(),
                document.getRemplacePar());
    }
}
