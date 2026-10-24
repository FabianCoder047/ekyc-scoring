package com.sfd.kyc.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.common.error.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        ProblemeSecurite.ecrire(
                response,
                objectMapper,
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCodes.TOKEN_INVALIDE,
                "Authentification requise.");
    }
}
