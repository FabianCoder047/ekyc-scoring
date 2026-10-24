package com.sfd.kyc.admin.web;

import com.sfd.kyc.kyc.domain.RegleScoring;
import java.math.BigDecimal;

public record CritereDto(
        Long id,
        String critere,
        BigDecimal borneMin,
        BigDecimal borneMax,
        int points,
        String libelle,
        boolean actif) {

    public static CritereDto de(RegleScoring regle) {
        return new CritereDto(
                regle.getId(),
                regle.getCritere().name(),
                regle.getBorneMin(),
                regle.getBorneMax(),
                regle.getPoints(),
                regle.getLibelle(),
                regle.isActif());
    }
}
