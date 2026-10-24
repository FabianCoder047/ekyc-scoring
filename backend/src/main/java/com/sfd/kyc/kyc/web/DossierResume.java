package com.sfd.kyc.kyc.web;

import com.sfd.kyc.common.util.Masquage;
import com.sfd.kyc.kyc.domain.DossierKyc;
import java.time.Instant;
import java.time.LocalDate;

public record DossierResume(
        Long id,
        Long clientId,
        String clientNom,
        String clientPrenom,
        Long agenceId,
        String agenceNom,
        int version,
        String statut,
        String typePiece,
        String numeroPieceMasque,
        LocalDate dateExpirationPiece,
        String niveauRisque,
        Instant creeLe,
        Instant soumisLe) {

    public static DossierResume de(DossierKyc dossier) {
        return new DossierResume(
                dossier.getId(),
                dossier.getClient().getId(),
                dossier.getClient().getNom(),
                dossier.getClient().getPrenom(),
                dossier.getAgence().getId(),
                dossier.getAgence().getNom(),
                dossier.getVersion(),
                dossier.getStatut().name(),
                dossier.getTypePiece().name(),
                Masquage.numeroPiece(dossier.getNumeroPiece()),
                dossier.getDateExpirationPiece(),
                dossier.getNiveauRisque() == null ? null : dossier.getNiveauRisque().name(),
                dossier.getCreeLe(),
                dossier.getSoumisLe());
    }
}
