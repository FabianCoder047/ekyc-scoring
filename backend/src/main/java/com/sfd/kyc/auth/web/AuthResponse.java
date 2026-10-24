package com.sfd.kyc.auth.web;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UtilisateurDto utilisateur) {
}
