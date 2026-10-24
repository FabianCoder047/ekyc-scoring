package com.sfd.kyc.kyc.web;

import com.sfd.kyc.kyc.domain.TypePiece;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DossierCreationRequest(
        @NotNull(message = "Le client est obligatoire")
        Long clientId,

        TypePiece typePiece,

        @NotBlank(message = "Le numéro de pièce est obligatoire")
        @Size(max = 40, message = "Le numéro de pièce ne doit pas dépasser 40 caractères")
        String numeroPiece,

        @NotNull(message = "La date d'expiration de la pièce est obligatoire")
        LocalDate dateExpirationPiece,

        @PositiveOrZero(message = "Le revenu doit être positif ou nul")
        BigDecimal revenuMensuel,

        @PositiveOrZero(message = "Les charges doivent être positives ou nulles")
        BigDecimal chargesMensuelles,

        @PositiveOrZero(message = "L'ancienneté doit être positive ou nulle")
        Integer ancienneteActiviteMois,

        @PositiveOrZero(message = "Le nombre de personnes à charge doit être positif ou nul")
        Integer nbPersonnesCharge) {
}
