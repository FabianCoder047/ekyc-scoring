package com.sfd.kyc.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.common.audit.AuditService;
import com.sfd.kyc.common.error.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        try {
            auditService.accesRefuse(request.getMethod() + " " + request.getRequestURI(), "ROUTE", null);
        } catch (RuntimeException e) {
            // Un incident d'audit ne doit pas transformer un 403 en 500.
        }
        ProblemeSecurite.ecrire(
                response,
                objectMapper,
                HttpStatus.FORBIDDEN.value(),
                ErrorCodes.ACCES_REFUSE,
                "Accès refusé.");
    }
}
