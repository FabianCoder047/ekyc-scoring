package com.sfd.kyc.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;

final class ProblemeSecurite {

    private ProblemeSecurite() {
    }

    static void ecrire(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int statut,
            String code,
            String detail) throws IOException {
        response.setStatus(statut);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("type", "about:blank");
        corps.put("title", "Erreur d'authentification ou d'autorisation");
        corps.put("status", statut);
        corps.put("detail", detail);
        corps.put("code", code);
        objectMapper.writeValue(response.getWriter(), corps);
    }
}
