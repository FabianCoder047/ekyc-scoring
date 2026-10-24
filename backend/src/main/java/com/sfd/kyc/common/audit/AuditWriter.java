package com.sfd.kyc.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.common.util.Hachage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditWriter {

    /** Verrou consultatif stable pour sérialiser l'écriture de la chaîne. */
    private static final long VERROU_AUDIT = 742001L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void append(
            String action,
            String entiteType,
            Long entiteId,
            Object avant,
            Object apres,
            Long utilisateurId,
            String role,
            String adresseIp,
            String userAgent) {
        inserer(action, entiteType, entiteId, avant, apres, utilisateurId, role, adresseIp, userAgent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendNouvelleTransaction(
            String action,
            String entiteType,
            Long entiteId,
            Object avant,
            Object apres,
            Long utilisateurId,
            String role,
            String adresseIp,
            String userAgent) {
        inserer(action, entiteType, entiteId, avant, apres, utilisateurId, role, adresseIp, userAgent);
    }

    private void inserer(
            String action,
            String entiteType,
            Long entiteId,
            Object avant,
            Object apres,
            Long utilisateurId,
            String role,
            String adresseIp,
            String userAgent) {
        // Sérialise la chaîne : le verrou est relâché à la fin de la transaction.
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(" + VERROU_AUDIT + ")", rs -> {
        });

        String hashPrecedent = jdbcTemplate.query(
                "SELECT hash FROM audit_log ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : Hachage.hashPrecedentInitial());

        Instant horodatage = clock.instant().truncatedTo(ChronoUnit.MICROS);
        String avantJson = json(avant);
        String apresJson = json(apres);

        String canonique = AuditCanonical.construire(
                horodatage, utilisateurId, role, action, entiteType, entiteId,
                avantJson, apresJson, adresseIp, userAgent);
        String hash = Hachage.sha256Hex(hashPrecedent + canonique);

        jdbcTemplate.update("""
                INSERT INTO audit_log
                  (horodatage, utilisateur_id, role, action, entite_type, entite_id,
                   avant, apres, adresse_ip, user_agent, hash_precedent, hash)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?)
                """,
                java.sql.Timestamp.from(horodatage),
                utilisateurId,
                role,
                action,
                entiteType,
                entiteId,
                avantJson,
                apresJson,
                adresseIp,
                userAgent,
                hashPrecedent,
                hash);
    }

    private String json(Object valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser le contenu d'audit", e);
        }
    }

    static Instant horodatage(ResultSet rs, String colonne) throws SQLException {
        java.time.OffsetDateTime valeur = rs.getObject(colonne, java.time.OffsetDateTime.class);
        return valeur == null ? null : valeur.toInstant();
    }
}
