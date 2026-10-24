package com.sfd.kyc.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Représentation canonique d'une ligne d'audit. Les objets JSON sont triés par
 * clé afin que la sérialisation Java et la normalisation JSONB de PostgreSQL
 * produisent exactement la même chaîne.
 */
public final class AuditCanonical {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditCanonical() {
    }

    public static String construire(
            Instant horodatage,
            Long utilisateurId,
            String role,
            String action,
            String entiteType,
            Long entiteId,
            String avantJson,
            String apresJson,
            String adresseIp,
            String userAgent) {
        StringBuilder sb = new StringBuilder();
        ajouter(sb, DateTimeFormatter.ISO_INSTANT.format(horodatage));
        ajouter(sb, utilisateurId == null ? null : utilisateurId.toString());
        ajouter(sb, role);
        ajouter(sb, action);
        ajouter(sb, entiteType);
        ajouter(sb, entiteId == null ? null : entiteId.toString());
        ajouter(sb, normaliserJson(avantJson));
        ajouter(sb, normaliserJson(apresJson));
        ajouter(sb, adresseIp);
        ajouter(sb, userAgent);
        return sb.toString();
    }

    private static void ajouter(StringBuilder sb, String valeur) {
        if (valeur == null) {
            sb.append("-1:");
        } else {
            sb.append(valeur.length()).append(':').append(valeur);
        }
    }

    private static String normaliserJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return MAPPER.writeValueAsString(trier(node));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Contenu JSON d'audit invalide", e);
        }
    }

    private static JsonNode trier(JsonNode node) {
        if (node.isObject()) {
            ObjectNode resultat = MAPPER.createObjectNode();
            List<String> noms = new ArrayList<>();
            node.fieldNames().forEachRemaining(noms::add);
            Collections.sort(noms);
            for (String nom : noms) {
                resultat.set(nom, trier(node.get(nom)));
            }
            return resultat;
        }
        if (node.isArray()) {
            ArrayNode resultat = MAPPER.createArrayNode();
            node.forEach(element -> resultat.add(trier(element)));
            return resultat;
        }
        return node;
    }
}
