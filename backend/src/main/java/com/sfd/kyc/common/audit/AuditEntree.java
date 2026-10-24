package com.sfd.kyc.common.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record AuditEntree(
        Long id,
        Instant horodatage,
        Long utilisateurId,
        String role,
        String action,
        String entiteType,
        Long entiteId,
        JsonNode avant,
        JsonNode apres,
        String adresseIp,
        String userAgent,
        String hashPrecedent,
        String hash) {
}
