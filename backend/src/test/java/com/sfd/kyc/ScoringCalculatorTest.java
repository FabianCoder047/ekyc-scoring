package com.sfd.kyc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfd.kyc.kyc.domain.CritereScoring;
import com.sfd.kyc.kyc.domain.PalierScoring;
import com.sfd.kyc.kyc.domain.RegleScoring;
import com.sfd.kyc.kyc.service.ScoringCalculator;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringCalculatorTest {

    private final ScoringCalculator calculator = new ScoringCalculator();

    @Test
    void calcule_le_jeu_de_test_du_cahier_des_charges() {
        Map<CritereScoring, BigDecimal> valeurs = new EnumMap<>(CritereScoring.class);
        valeurs.put(CritereScoring.RATIO_ENDETTEMENT, new BigDecimal("24.0000"));
        valeurs.put(CritereScoring.ANCIENNETE_ACTIVITE, new BigDecimal("24"));
        valeurs.put(CritereScoring.REVENU_MENSUEL, new BigDecimal("250000"));

        Map<CritereScoring, Boolean> neutres = new EnumMap<>(CritereScoring.class);
        neutres.put(CritereScoring.HISTORIQUE_REMBOURSEMENT, true);

        ScoringCalculator.ResultatCalcul resultat =
                calculator.calculer(valeurs, neutres, regles());

        assertThat(resultat.score()).isEqualTo(67);
        assertThat(resultat.palier()).isEqualTo(PalierScoring.A_ETUDIER);

        assertThat(resultat.detail())
                .filteredOn(detail -> detail.critere().equals("RATIO_ENDETTEMENT"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.points()).isEqualTo(25));
        assertThat(resultat.detail())
                .filteredOn(detail -> detail.critere().equals("ANCIENNETE_ACTIVITE"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.points()).isEqualTo(15));
        assertThat(resultat.detail())
                .filteredOn(detail -> detail.critere().equals("REVENU_MENSUEL"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.points()).isEqualTo(15));
        assertThat(resultat.detail())
                .filteredOn(detail -> detail.critere().equals("HISTORIQUE_REMBOURSEMENT"))
                .singleElement()
                .satisfies(detail -> assertThat(detail.points()).isEqualTo(12));
    }

    @Test
    void les_bornes_sont_incluses() {
        Map<CritereScoring, BigDecimal> valeurs = new EnumMap<>(CritereScoring.class);
        valeurs.put(CritereScoring.RATIO_ENDETTEMENT, new BigDecimal("30"));
        valeurs.put(CritereScoring.ANCIENNETE_ACTIVITE, new BigDecimal("36"));
        valeurs.put(CritereScoring.REVENU_MENSUEL, new BigDecimal("300000"));
        valeurs.put(CritereScoring.HISTORIQUE_REMBOURSEMENT, new BigDecimal("10"));

        ScoringCalculator.ResultatCalcul resultat =
                calculator.calculer(valeurs, Map.of(), regles());

        assertThat(resultat.score()).isEqualTo(25 + 25 + 25 + 15);
        assertThat(resultat.palier()).isEqualTo(PalierScoring.FAVORABLE);
    }

    private List<RegleScoring> regles() {
        return List.of(
                regle(CritereScoring.RATIO_ENDETTEMENT, "0", "30", 25),
                regle(CritereScoring.RATIO_ENDETTEMENT, "30.0001", "50", 15),
                regle(CritereScoring.RATIO_ENDETTEMENT, "50.0001", null, 0),
                regle(CritereScoring.ANCIENNETE_ACTIVITE, "0", "11", 5),
                regle(CritereScoring.ANCIENNETE_ACTIVITE, "12", "35", 15),
                regle(CritereScoring.ANCIENNETE_ACTIVITE, "36", null, 25),
                regle(CritereScoring.REVENU_MENSUEL, "0", "99999.9999", 8),
                regle(CritereScoring.REVENU_MENSUEL, "100000", "299999.9999", 15),
                regle(CritereScoring.REVENU_MENSUEL, "300000", null, 25),
                regle(CritereScoring.HISTORIQUE_REMBOURSEMENT, "0", "0", 25),
                regle(CritereScoring.HISTORIQUE_REMBOURSEMENT, "0.0001", "20", 15),
                regle(CritereScoring.HISTORIQUE_REMBOURSEMENT, "20.0001", null, 0));
    }

    private RegleScoring regle(
            CritereScoring critere, String min, String max, int points) {
        RegleScoring regle = new RegleScoring();
        regle.setCritere(critere);
        regle.setBorneMin(min == null ? null : new BigDecimal(min));
        regle.setBorneMax(max == null ? null : new BigDecimal(max));
        regle.setPoints(points);
        regle.setLibelle("libellé");
        regle.setActif(true);
        return regle;
    }
}
