package com.sfd.kyc.kyc.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.kyc.domain.ResultatScoring;
import java.time.Instant;

public record ResultatScoringDto(
        Long id,
        int score,
        String palier,
        JsonNode detail,
        UtilisateurRef calculePar,
        Instant calculeLe) {

    public static ResultatScoringDto de(ResultatScoring resultat, ObjectMapper objectMapper) {
        JsonNode detail = null;
        if (resultat.getDetail() != null) {
            try {
                detail = objectMapper.readTree(resultat.getDetail());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("Détail de scoring illisible", e);
            }
        }
        return new ResultatScoringDto(
                resultat.getId(),
                resultat.getScore(),
                resultat.getPalier().name(),
                detail,
                UtilisateurRef.de(resultat.getCalculePar()),
                resultat.getCalculeLe());
    }
}
