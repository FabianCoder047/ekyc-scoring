package com.sfd.kyc.kyc.service;

import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import com.sfd.kyc.kyc.domain.CritereScoring;
import com.sfd.kyc.kyc.domain.PalierScoring;
import com.sfd.kyc.kyc.domain.RegleScoring;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ScoringCalculator {

    /** Points attribués à un client sans historique de remboursement (RG-S3 / section 8). */
    public static final int POINTS_HISTORIQUE_NEUTRE = 12;

    public ResultatCalcul calculer(
            Map<CritereScoring, BigDecimal> valeurs,
            Map<CritereScoring, Boolean> neutres,
            List<RegleScoring> regles) {
        List<DetailCritere> details = new ArrayList<>();
        int total = 0;

        for (CritereScoring critere : CritereScoring.values()) {
            if (Boolean.TRUE.equals(neutres.get(critere))) {
                details.add(new DetailCritere(
                        critere.name(), null, POINTS_HISTORIQUE_NEUTRE,
                        "Historique non disponible (valeur neutre)", null, null));
                total += POINTS_HISTORIQUE_NEUTRE;
                continue;
            }

            BigDecimal valeur = valeurs.get(critere);
            if (valeur == null) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.SCORING_DONNEES_INCOMPLETES,
                        "Le critère " + critere.name() + " est incomplet.");
            }

            RegleScoring regle = regles.stream()
                    .filter(r -> r.getCritere() == critere && r.isActif())
                    .filter(r -> correspond(valeur, r))
                    .min(Comparator.comparing(
                            RegleScoring::getBorneMin,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            ErrorCodes.CRITERE_SCORING_MANQUANT,
                            "Aucune tranche de scoring ne couvre le critère " + critere.name() + "."));

            details.add(new DetailCritere(
                    critere.name(),
                    valeur.stripTrailingZeros(),
                    regle.getPoints(),
                    regle.getLibelle(),
                    regle.getBorneMin(),
                    regle.getBorneMax()));
            total += regle.getPoints();
        }

        if (total < 0 || total > 100) {
            throw new IllegalStateException("Score hors bornes : " + total);
        }
        return new ResultatCalcul(total, PalierScoring.depuisScore(total), List.copyOf(details));
    }

    private boolean correspond(BigDecimal valeur, RegleScoring regle) {
        boolean auDessus = regle.getBorneMin() == null || valeur.compareTo(regle.getBorneMin()) >= 0;
        boolean enDessous = regle.getBorneMax() == null || valeur.compareTo(regle.getBorneMax()) <= 0;
        return auDessus && enDessous;
    }

    /** Les bornes sont inclusives ; la première tranche par borneMin croissante gagne. */
    public record DetailCritere(
            String critere,
            BigDecimal valeur,
            int points,
            String libelle,
            BigDecimal borneMin,
            BigDecimal borneMax) {
    }

    public record ResultatCalcul(int score, PalierScoring palier, List<DetailCritere> detail) {
    }

    public static Map<CritereScoring, Boolean> neutresVides() {
        return new EnumMap<>(CritereScoring.class);
    }
}
