package com.sfd.kyc.common.security;

import com.sfd.kyc.common.error.ApiException;
import com.sfd.kyc.common.error.ErrorCodes;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UtilisateurAuthentifie> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof UtilisateurAuthentifie utilisateur) {
            return Optional.of(utilisateur);
        }
        return Optional.empty();
    }

    public static UtilisateurAuthentifie get() {
        return optional().orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED, ErrorCodes.TOKEN_INVALIDE, "Authentification requise."));
    }
}
