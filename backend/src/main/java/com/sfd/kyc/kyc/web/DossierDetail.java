package com.sfd.kyc.kyc.web;

import com.sfd.kyc.common.util.Masquage;
import com.sfd.kyc.kyc.domain.DossierKyc;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DossierDetail(
        Long id,
        Long clientId,
        String clientNom,
        String clientPrenom,
        Long agenceId,
        String agenceNom,
        int version,
        String statut,
        String typePiece,
        String numeroPiece,
        String numeroPieceMasque,
        LocalDate dateExpirationPiece,
        BigDecimal revenuMensuel,
        BigDecimal chargesMensuelles,
        Integer ancienneteActiviteMois,
        Integer nbPersonnesCharge,
        String niveauRisque,
        UtilisateurRef creePar,
        UtilisateurRef soumisPar,
        Instant soumisLe,
        UtilisateurRef assigneA,
        UtilisateurRef decidePar,
        Instant decideLe,
        String motifRejet,
        Instant creeLe,
        List<DocumentDto> documents) {

    public static DossierDetail de(DossierKyc dossier, boolean numeroEnClair, List<DocumentDto> documents) {
        return new DossierDetail(
                dossier.getId(),
                dossier.getClient().getId(),
                dossier.getClient().getNom(),
                dossier.getClient().getPrenom(),
                dossier.getAgence().getId(),
                dossier.getAgence().getNom(),
                dossier.getVersion(),
                dossier.getStatut().name(),
                dossier.getTypePiece().name(),
                numeroEnClair ? dossier.getNumeroPiece() : null,
                Masquage.numeroPiece(dossier.getNumeroPiece()),
                dossier.getDateExpirationPiece(),
                dossier.getRevenuMensuel(),
                dossier.getChargesMensuelles(),
                dossier.getAncienneteActiviteMois(),
                dossier.getNbPersonnesCharge(),
                dossier.getNiveauRisque() == null ? null : dossier.getNiveauRisque().name(),
                UtilisateurRef.de(dossier.getCreePar()),
                UtilisateurRef.de(dossier.getSoumisPar()),
                dossier.getSoumisLe(),
                UtilisateurRef.de(dossier.getAssigneA()),
                UtilisateurRef.de(dossier.getDecidePar()),
                dossier.getDecideLe(),
                dossier.getMotifRejet(),
                dossier.getCreeLe(),
                documents);
    }
}
