package com.sfd.kyc.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfd.kyc.common.util.Hachage;
import com.sfd.kyc.common.web.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEntree> lister(
            String entite, String action, Instant du, Instant au, int page, int taille) {
        int pageSure = Math.max(page, 0);
        int tailleSure = Math.min(Math.max(taille, 1), 100);

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parametres = new ArrayList<>();
        if (entite != null && !entite.isBlank()) {
            where.append(" AND entite_type = ?");
            parametres.add(entite.trim().toUpperCase(Locale.ROOT));
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ?");
            parametres.add(action.trim().toUpperCase(Locale.ROOT));
        }
        if (du != null) {
            where.append(" AND horodatage >= ?");
            parametres.add(java.sql.Timestamp.from(du));
        }
        if (au != null) {
            where.append(" AND horodatage <= ?");
            parametres.add(java.sql.Timestamp.from(au));
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log" + where, Long.class, parametres.toArray());

        List<Object> args = new ArrayList<>(parametres);
        args.add(tailleSure);
        args.add((long) pageSure * tailleSure);

        List<AuditEntree> entrees = jdbcTemplate.query(
                "SELECT * FROM audit_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapper(rs),
                args.toArray());

        return PageResponse.de(entrees, pageSure, tailleSure, total == null ? 0 : total);
    }

    /**
     * Recalcule toute la chaîne. La première rupture (hash précédent ou hash
     * recalculé incorrect) identifie la première ligne invalide.
     */
    @Transactional(readOnly = true)
    public VerificationAudit verifier() {
        List<LigneAudit> lignes = jdbcTemplate.query(
                """
                SELECT id, horodatage, utilisateur_id, role, action, entite_type, entite_id,
                       avant::text AS avant, apres::text AS apres, adresse_ip, user_agent,
                       hash_precedent, hash
                FROM audit_log ORDER BY id ASC
                """,
                (rs, rowNum) -> new LigneAudit(
                        rs.getLong("id"),
                        rs.getObject("horodatage", java.time.OffsetDateTime.class).toInstant(),
                        rs.getObject("utilisateur_id", Long.class),
                        rs.getString("role"),
                        rs.getString("action"),
                        rs.getString("entite_type"),
                        rs.getObject("entite_id", Long.class),
                        rs.getString("avant"),
                        rs.getString("apres"),
                        rs.getString("adresse_ip"),
                        rs.getString("user_agent"),
                        rs.getString("hash_precedent"),
                        rs.getString("hash")));

        String attendu = Hachage.hashPrecedentInitial();
        long verifiees = 0;
        for (LigneAudit ligne : lignes) {
            verifiees++;
            if (!Hachage.egalConstant(attendu, ligne.hashPrecedent())) {
                return new VerificationAudit(false, ligne.id(), verifiees);
            }
            String canonique = AuditCanonical.construire(
                    ligne.horodatage(),
                    ligne.utilisateurId(),
                    ligne.role(),
                    ligne.action(),
                    ligne.entiteType(),
                    ligne.entiteId(),
                    ligne.avant(),
                    ligne.apres(),
                    ligne.adresseIp(),
                    ligne.userAgent());
            String recalcul = Hachage.sha256Hex(ligne.hashPrecedent() + canonique);
            if (!Hachage.egalConstant(recalcul, ligne.hash())) {
                return new VerificationAudit(false, ligne.id(), verifiees);
            }
            attendu = ligne.hash();
        }
        return new VerificationAudit(true, null, verifiees);
    }

    private AuditEntree mapper(ResultSet rs) throws SQLException {
        return new AuditEntree(
                rs.getLong("id"),
                rs.getObject("horodatage", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("utilisateur_id", Long.class),
                rs.getString("role"),
                rs.getString("action"),
                rs.getString("entite_type"),
                rs.getObject("entite_id", Long.class),
                json(rs.getString("avant")),
                json(rs.getString("apres")),
                rs.getString("adresse_ip"),
                rs.getString("user_agent"),
                rs.getString("hash_precedent"),
                rs.getString("hash"));
    }

    private JsonNode json(String contenu) throws SQLException {
        if (contenu == null) {
            return null;
        }
        try {
            return objectMapper.readTree(contenu);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON d'audit invalide", e);
        }
    }

    private record LigneAudit(
            Long id,
            Instant horodatage,
            Long utilisateurId,
            String role,
            String action,
            String entiteType,
            Long entiteId,
            String avant,
            String apres,
            String adresseIp,
            String userAgent,
            String hashPrecedent,
            String hash) {
    }
}
