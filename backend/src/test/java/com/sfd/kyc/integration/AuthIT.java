package com.sfd.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.sfd.kyc.auth.domain.Agence;
import com.sfd.kyc.auth.domain.Role;
import com.sfd.kyc.auth.domain.Utilisateur;
import com.sfd.kyc.auth.repo.AgenceRepository;
import com.sfd.kyc.auth.repo.UtilisateurRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

class AuthIT extends AbstractIntegrationTest {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void connexion_reussie_et_jeton_utilisable() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "agent@sfd.tg", "password", "Demo123456!"))))
                .andReturn();

        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        JsonNode corps = json(resultat);
        assertThat(corps.path("accessToken").asText()).isNotBlank();
        assertThat(corps.path("refreshToken").asText()).isNotBlank();
        assertThat(corps.path("expiresIn").asLong()).isEqualTo(900);
        assertThat(corps.path("utilisateur").path("role").asText()).isEqualTo("AGENT_ACCUEIL");
    }

    @Test
    void le_message_d_erreur_ne_revele_pas_l_existence_du_compte() throws Exception {
        MvcResult inconnu = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "inconnu@sfd.tg", "password", "MauvaisMotDePasse!"))))
                .andReturn();
        MvcResult mauvaisMotDePasse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "analyste@sfd.tg", "password", "MauvaisMotDePasse!"))))
                .andReturn();

        assertThat(inconnu.getResponse().getStatus()).isEqualTo(401);
        assertThat(mauvaisMotDePasse.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(inconnu).path("detail").asText())
                .isEqualTo(json(mauvaisMotDePasse).path("detail").asText())
                .isEqualTo("Email ou mot de passe invalide.");
        assertThat(json(inconnu).path("code").asText()).isEqualTo("AUTHENTIFICATION_ECHOUEE");
    }

    @Test
    void cinq_echecs_bloquent_meme_le_bon_mot_de_passe() throws Exception {
        Agence agence = agenceRepository.findAll().getFirst();
        String email = "brute-" + UUID.randomUUID() + "@sfd.tg";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasseHash(passwordEncoder.encode("BonMotDePasse!123"));
        utilisateur.setNom("TEST");
        utilisateur.setPrenom("Force brute");
        utilisateur.setRole(Role.AGENT_ACCUEIL);
        utilisateur.setAgence(agence);
        utilisateurRepository.saveAndFlush(utilisateur);

        for (int tentative = 1; tentative <= 5; tentative++) {
            MvcResult echec = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", email, "password", "MauvaisMotDePasse!"))))
                    .andReturn();
            assertThat(echec.getResponse().getStatus()).isEqualTo(401);
        }

        MvcResult bloque = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "BonMotDePasse!123"))))
                .andReturn();

        assertThat(bloque.getResponse().getStatus()).isEqualTo(429);
        assertThat(json(bloque).path("code").asText()).isEqualTo("TROP_DE_TENTATIVES");
    }

    @Test
    void le_refresh_token_est_rotationne_et_ne_peut_pas_etre_reutilise() throws Exception {
        MvcResult connexion = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "analyste@sfd.tg", "password", "Demo123456!"))))
                .andReturn();
        String ancienRefresh = json(connexion).path("refreshToken").asText();

        MvcResult rotation = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ancienRefresh))))
                .andReturn();
        assertThat(rotation.getResponse().getStatus()).isEqualTo(200);
        String nouveauRefresh = json(rotation).path("refreshToken").asText();
        assertThat(nouveauRefresh).isNotEqualTo(ancienRefresh);

        MvcResult reutilisation = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", ancienRefresh))))
                .andReturn();
        assertThat(reutilisation.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(reutilisation).path("code").asText()).isEqualTo("REFRESH_TOKEN_INVALIDE");

        // La réutilisation révoque toute la famille, y compris le jeton courant.
        MvcResult apresReutilisation = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", nouveauRefresh))))
                .andReturn();
        assertThat(apresReutilisation.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void la_deconnexion_revoque_le_refresh_token() throws Exception {
        String access = login("superviseur@sfd.tg", "Demo123456!");
        String refresh = refresh("superviseur@sfd.tg", "Demo123456!");

        MvcResult deconnexion = mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andReturn();
        assertThat(deconnexion.getResponse().getStatus()).isEqualTo(204);

        MvcResult apres = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andReturn();
        assertThat(apres.getResponse().getStatus()).isEqualTo(401);
    }
}
