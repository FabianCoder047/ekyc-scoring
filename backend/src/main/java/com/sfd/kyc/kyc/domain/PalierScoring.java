package com.sfd.kyc.kyc.domain;

public enum PalierScoring {
    FAVORABLE,
    A_ETUDIER,
    DEFAVORABLE;

    public static PalierScoring depuisScore(int score) {
        if (score >= 70) {
            return FAVORABLE;
        }
        if (score >= 50) {
            return A_ETUDIER;
        }
        return DEFAVORABLE;
    }
}
