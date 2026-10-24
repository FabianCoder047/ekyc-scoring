package com.sfd.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void la_chaine_d_audit_est_valide_et_filtrable() throws Exception {
        String superviseur = login("superviseur@sfd.tg", "Demo123456!");

        MvcResult verification = mockMvc.perform(get("/audit/verification")
                        .header("Authorization", "Bearer " + superviseur))
                .andReturn();
        assertThat(verification.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(verification).path("valide").asBoolean()).isTrue();

        MvcResult liste = mockMvc.perform(get("/audit")
                        .param("action", "LOGIN_SUCCES")
                        .param("taille", "5")
                        .header("Authorization", "Bearer " + superviseur))
                .andReturn();
        assertThat(liste.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(liste).path("contenu")).isNotEmpty();
        assertThat(json(liste).path("contenu").get(0).path("action").asText())
                .isEqualTo("LOGIN_SUCCES");
    }

    @Test
    @Order(2)
    void un_agent_ne_peut_pas_lire_l_audit_et_le_refus_est_journalise() throws Exception {
        Long avant = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'ACCES_REFUSE'", Long.class);

        String agent = login("agent@sfd.tg", "Demo123456!");
        MvcResult refus = mockMvc.perform(get("/audit")
                        .header("Authorization", "Bearer " + agent))
                .andReturn();
        assertThat(refus.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(refus).path("code").asText()).isEqualTo("ACCES_REFUSE");

        Long apres = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'ACCES_REFUSE'", Long.class);
        assertThat(apres).isEqualTo(avant + 1);
    }

    @Test
    @Order(3)
    void les_numeros_de_piece_sont_masques_dans_les_listes() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        Long clientId = jdbcTemplate.queryForObject(
                "SELECT id FROM client WHERE telephone = '+22890000002'", Long.class);
        String numeroClair = "TG9999888877776";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kyc/dossiers")
                        .header("Authorization", "Bearer " + agent)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "clientId", clientId,
                                "typePiece", "CNI",
                                "numeroPiece", numeroClair,
                                "dateExpirationPiece", "2032-12-31"))))
                .andReturn();

        String analyste = login("analyste@sfd.tg", "Demo123456!");

        MvcResult liste = mockMvc.perform(get("/kyc/dossiers")
                        .param("taille", "100")
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(liste.getResponse().getStatus()).isEqualTo(200);
        assertThat(liste.getResponse().getContentAsString()).doesNotContain(numeroClair);
        assertThat(liste.getResponse().getContentAsString()).contains("****");
    }

    @Test
    @Order(9)
    void le_trigger_bloque_toute_modification_d_une_ligne_d_audit() {
        Long id = jdbcTemplate.queryForObject("SELECT max(id) FROM audit_log", Long.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE audit_log SET role = 'ADMIN' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("TABLE_APPEND_ONLY");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_log WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("TABLE_APPEND_ONLY");
    }

    @Test
    @Order(10)
    void une_alteration_manuelle_rend_la_verification_invalide() throws Exception {
        String superviseur = login("superviseur@sfd.tg", "Demo123456!");
        Long id = jdbcTemplate.queryForObject("SELECT max(id) FROM audit_log", Long.class);

        jdbcTemplate.execute("ALTER TABLE audit_log DISABLE TRIGGER trg_audit_log_append_only");
        try {
            jdbcTemplate.update("UPDATE audit_log SET role = 'ADMIN' WHERE id = ?", id);
        } finally {
            jdbcTemplate.execute("ALTER TABLE audit_log ENABLE TRIGGER trg_audit_log_append_only");
        }

        MvcResult verification = mockMvc.perform(get("/audit/verification")
                        .header("Authorization", "Bearer " + superviseur))
                .andReturn();
        JsonNode corps = json(verification);
        assertThat(corps.path("valide").asBoolean()).isFalse();
        assertThat(corps.path("premiereLigneInvalide").asLong()).isEqualTo(id);
    }
}
