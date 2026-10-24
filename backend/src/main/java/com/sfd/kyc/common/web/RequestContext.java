package com.sfd.kyc.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContext {

    private RequestContext() {
    }

    public static Optional<HttpServletRequest> courante() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }

    /**
     * On utilise l'adresse de la connexion TCP. X-Forwarded-For n'est pas pris en
     * compte par défaut car il est falsifiable sans proxy de confiance.
     */
    public static String adresseIp() {
        return courante().map(HttpServletRequest::getRemoteAddr).orElse(null);
    }

    public static String userAgent() {
        return courante()
                .map(request -> request.getHeader("User-Agent"))
                .map(agent -> agent.length() > 200 ? agent.substring(0, 200) : agent)
                .orElse(null);
    }
}
