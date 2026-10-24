package com.sfd.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ContextIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void le_schema_et_le_seed_sont_charges() {
        Integer utilisateurs = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM utilisateur", Integer.class);
        Integer criteres = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM critere_scoring", Integer.class);

        assertThat(utilisateurs).isGreaterThanOrEqualTo(4);
        assertThat(criteres).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM utilisateur WHERE email IN "
                        + "('agent@sfd.tg','analyste@sfd.tg','superviseur@sfd.tg','admin@sfd.tg')",
                Integer.class)).isEqualTo(4);
    }
}
