package com.sfd.kyc;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfd.kyc.common.audit.AuditCanonical;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditCanonicalTest {

    private static final Instant HORODATAGE = Instant.parse("2026-01-15T10:30:00.123456Z");

    @Test
    void l_ordre_des_cles_json_ne_change_pas_l_empreinte() {
        String premier = AuditCanonical.construire(
                HORODATAGE, 3L, "SUPERVISEUR", "DOSSIER_VALIDE", "DOSSIER_KYC", 12L,
                null, "{\"statut\":\"VALIDE\",\"decision\":\"VALIDE\"}", "127.0.0.1", "junit");
        String second = AuditCanonical.construire(
                HORODATAGE, 3L, "SUPERVISEUR", "DOSSIER_VALIDE", "DOSSIER_KYC", 12L,
                null, "{\"decision\":\"VALIDE\",\"statut\":\"VALIDE\"}", "127.0.0.1", "junit");

        assertThat(premier).isEqualTo(second);
    }

    @Test
    void un_changement_de_contenu_change_l_empreinte() {
        String premier = AuditCanonical.construire(
                HORODATAGE, 3L, "ADMIN", "CRITERE_MODIFIE", "CRITERE_SCORING", 1L,
                null, "{\"points\":25}", null, null);
        String second = AuditCanonical.construire(
                HORODATAGE, 3L, "ADMIN", "CRITERE_MODIFIE", "CRITERE_SCORING", 1L,
                null, "{\"points\":15}", null, null);

        assertThat(premier).isNotEqualTo(second);
    }
}
