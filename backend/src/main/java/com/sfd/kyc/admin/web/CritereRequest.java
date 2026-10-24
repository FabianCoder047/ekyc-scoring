package com.sfd.kyc.admin.web;

import com.sfd.kyc.kyc.domain.CritereScoring;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CritereRequest(
        @NotNull(message = "Le critère est obligatoire")
        CritereScoring critere,

        BigDecimal borneMin,

        BigDecimal borneMax,

        @Min(value = 0, message = "Les points doivent être compris entre 0 et 25")
        @Max(value = 25, message = "Les points doivent être compris entre 0 et 25")
        int points,

        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 120)
        String libelle,

        boolean actif) {
}
