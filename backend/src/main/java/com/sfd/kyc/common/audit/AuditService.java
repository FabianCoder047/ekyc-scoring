package com.sfd.kyc.common.audit;

import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.common.security.CurrentUser;
import com.sfd.kyc.common.security.UtilisateurAuthentifie;
import com.sfd.kyc.common.web.RequestContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditWriter writer;

    public AuditService(AuditWriter writer) {
        this.writer = writer;
    }

    /** Écriture dans la transaction courante : l'audit et l'action sont atomiques. */
    public void journaliser(String action, String entiteType, Long entiteId, Object avant, Object apres) {
        UtilisateurAuthentifie utilisateur = CurrentUser.optional().orElse(null);
        writer.append(
                action,
                entiteType,
                entiteId,
                avant,
                apres,
                utilisateur == null ? null : utilisateur.id(),
                utilisateur == null ? null : utilisateur.role().name(),
                RequestContext.adresseIp(),
                RequestContext.userAgent());
    }

    /** Écriture immédiate, indépendante de la transaction métier (ex. échec de connexion). */
    public void journaliserNouvelleTransaction(
            String action, String entiteType, Long entiteId, Object avant, Object apres) {
        UtilisateurAuthentifie utilisateur = CurrentUser.optional().orElse(null);
        journaliserPour(
                action,
                entiteType,
                entiteId,
                avant,
                apres,
                utilisateur == null ? null : utilisateur.id(),
                utilisateur == null ? null : utilisateur.role());
    }

    /** Écriture dans la transaction courante avec un utilisateur explicite (ex. connexion). */
    public void journaliserPourMemeTransaction(
            String action,
            String entiteType,
            Long entiteId,
            Object avant,
            Object apres,
            Long utilisateurId,
            Role role) {
        writer.append(
                action,
                entiteType,
                entiteId,
                avant,
                apres,
                utilisateurId,
                role == null ? null : role.name(),
                RequestContext.adresseIp(),
                RequestContext.userAgent());
    }

    public void journaliserPour(
            String action,
            String entiteType,
            Long entiteId,
            Object avant,
            Object apres,
            Long utilisateurId,
            Role role) {
        writer.appendNouvelleTransaction(
                action,
                entiteType,
                entiteId,
                avant,
                apres,
                utilisateurId,
                role == null ? null : role.name(),
                RequestContext.adresseIp(),
                RequestContext.userAgent());
    }

    /**
     * Un refus d'accès ne doit pas être perdu si la transaction métier est
     * annulée : il est journalisé dans une transaction séparée.
     */
    public void accesRefuse(String actionTentee, String entiteType, Long entiteId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actionTentee", actionTentee);
        journaliserNouvelleTransaction(AuditAction.ACCES_REFUSE, entiteType, entiteId, null, details);
    }
}
