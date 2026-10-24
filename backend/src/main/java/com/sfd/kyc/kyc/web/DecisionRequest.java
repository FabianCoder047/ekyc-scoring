package com.sfd.kyc.kyc.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotBlank(message = "La décision est obligatoire")
        String decision,

        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères")
        String motif) {
}
