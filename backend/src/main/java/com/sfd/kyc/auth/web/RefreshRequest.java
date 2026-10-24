package com.sfd.kyc.auth.web;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Le refresh token est obligatoire") String refreshToken) {
}
