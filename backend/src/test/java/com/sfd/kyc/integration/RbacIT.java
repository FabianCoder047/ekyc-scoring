package com.sfd.kyc.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Vérifie les règles RBAC au niveau des méthodes de service, en s'appuyant sur
 * Spring Security Test pour injecter une authentification sans passer par JWT.
 */
class RbacIT extends AbstractIntegrationTest {

    @Test
    @WithMockUser(username = "admin@sfd.tg", roles = "ADMIN")
    void un_admin_ne_peut_pas_consulter_les_dossiers_kyc() throws Exception {
        mockMvc.perform(get("/kyc/dossiers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void un_analyste_ne_peut_pas_acceder_au_journal_d_audit() throws Exception {
        mockMvc.perform(get("/audit").with(user("analyste@sfd.tg").roles("ANALYSTE_KYC")))
                .andExpect(status().isForbidden());
    }
}
