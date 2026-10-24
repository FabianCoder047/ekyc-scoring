package com.sfd.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class DossierWorkflowIT extends AbstractIntegrationTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nWQAAAAASUVORK5CYII=");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void parcours_complet_brouillon_vers_valide_avec_scoring_explicable() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long clientId = clientId("+22890000002");
        long dossierId = creerDossier(agent, clientId, "TG1234567891234", "250000", "60000", "24");
        deposerPiece(agent, dossierId);
        soumettre(agent, dossierId);

        String analyste = login("analyste@sfd.tg", "Demo123456!");
        MvcResult priseEnCharge = mockMvc.perform(post("/kyc/dossiers/{id}/prise-en-charge", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(priseEnCharge.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(priseEnCharge).path("statut").asText()).isEqualTo("EN_REVUE");
        assertThat(json(priseEnCharge).path("assigneA").path("nom").asText()).isEqualTo("MENSAH");

        MvcResult scoring = mockMvc.perform(post("/kyc/dossiers/{id}/scoring", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(scoring.getResponse().getStatus()).isEqualTo(200);
        JsonNode score = json(scoring);
        assertThat(score.path("score").asInt()).isEqualTo(67);
        assertThat(score.path("palier").asText()).isEqualTo("A_ETUDIER");
        assertThat(score.path("detail")).hasSize(4);
        assertThat(score.path("detail").get(0).path("points").asInt()).isBetween(0, 25);
        assertThat(score.path("detail").toString()).contains("RATIO_ENDETTEMENT");

        String superviseur = login("superviseur@sfd.tg", "Demo123456!");
        MvcResult decision = mockMvc.perform(post("/kyc/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + superviseur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "VALIDE"))))
                .andReturn();
        assertThat(decision.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(decision).path("statut").asText()).isEqualTo("VALIDE");

        MvcResult liste = mockMvc.perform(get("/kyc/dossiers")
                        .param("q", "MENSAH")
                        .header("Authorization", "Bearer " + agent))
                .andReturn();
        assertThat(liste.getResponse().getStatus()).isEqualTo(200);
        assertThat(liste.getResponse().getContentAsString()).contains("TG****1234");
        assertThat(liste.getResponse().getContentAsString()).doesNotContain("TG1234567891234");

        MvcResult detailAnalyste = mockMvc.perform(get("/kyc/dossiers/{id}", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(json(detailAnalyste).path("numeroPiece").asText()).isEqualTo("TG1234567891234");

        MvcResult historique = mockMvc.perform(get("/kyc/dossiers/{id}/historique", dossierId)
                        .header("Authorization", "Bearer " + superviseur))
                .andReturn();
        assertThat(json(historique).path("scorings")).hasSize(1);
        assertThat(json(historique).path("decisions")).hasSize(3);
    }

    @Test
    void quatre_yeux_et_refus_agent_sont_appliques_cote_serveur() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long clientId = clientId("+22890000002");
        long dossierId = creerDossier(agent, clientId, "TG9876543219876", "200000", "20000", "18");
        deposerPiece(agent, dossierId);
        soumettre(agent, dossierId);

        String analyste = login("analyste@sfd.tg", "Demo123456!");
        mockMvc.perform(post("/kyc/dossiers/{id}/prise-en-charge", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();

        // Un agent d'accueil ne peut pas décider : 403 + ACCES_REFUSE.
        MvcResult refusAgent = mockMvc.perform(post("/kyc/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "VALIDE"))))
                .andReturn();
        assertThat(refusAgent.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(refusAgent).path("code").asText()).isEqualTo("ACCES_REFUSE");

        Long superviseurId = jdbcTemplate.queryForObject(
                "SELECT id FROM utilisateur WHERE email = 'superviseur@sfd.tg'", Long.class);
        Long agentId = jdbcTemplate.queryForObject(
                "SELECT id FROM utilisateur WHERE email = 'agent@sfd.tg'", Long.class);
        // Simule le cas où le superviseur serait aussi le soumissionnaire.
        jdbcTemplate.update("UPDATE dossier_kyc SET soumis_par = ? WHERE id = ?", superviseurId, dossierId);

        String superviseur = login("superviseur@sfd.tg", "Demo123456!");
        MvcResult decision = mockMvc.perform(post("/kyc/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + superviseur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "VALIDE"))))
                .andReturn();
        assertThat(decision.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(decision).path("code").asText()).isEqualTo("QUATRE_YEUX");

        Integer refus = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'ACCES_REFUSE'", Integer.class);
        assertThat(refus).isGreaterThanOrEqualTo(2);
        Integer refusQuatreYeux = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'ACCES_REFUSE' AND entite_id = ?",
                Integer.class, dossierId);
        assertThat(refusQuatreYeux).isGreaterThanOrEqualTo(1);

        // Remise en état pour les autres tests.
        jdbcTemplate.update("UPDATE dossier_kyc SET soumis_par = ? WHERE id = ?", agentId, dossierId);
    }

    @Test
    void un_dossier_rejete_est_corrige_dans_une_nouvelle_version() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long clientId = nouveauClient("+2289" + System.nanoTime() % 100_000_000L);
        long dossierId = creerDossier(agent, clientId, "TG1111222233334", "150000", "90000", "8");
        deposerPiece(agent, dossierId);
        soumettre(agent, dossierId);

        String analyste = login("analyste@sfd.tg", "Demo123456!");
        mockMvc.perform(post("/kyc/dossiers/{id}/prise-en-charge", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();

        String superviseur = login("superviseur@sfd.tg", "Demo123456!");
        MvcResult rejet = mockMvc.perform(post("/kyc/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + superviseur)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("decision", "REJETE", "motif", "Pièce illisible"))))
                .andReturn();
        assertThat(rejet.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(rejet).path("statut").asText()).isEqualTo("REJETE");
        assertThat(json(rejet).path("motifRejet").asText()).isEqualTo("Pièce illisible");

        Map<String, Object> correction = new LinkedHashMap<>();
        correction.put("typePiece", "CNI");
        correction.put("numeroPiece", "TG5555666677778");
        correction.put("dateExpirationPiece", "2032-12-31");
        correction.put("revenuMensuel", 150000);
        correction.put("chargesMensuelles", 90000);
        correction.put("ancienneteActiviteMois", 8);
        correction.put("nbPersonnesCharge", 1);

        MvcResult nouvelleVersion = mockMvc.perform(put("/kyc/dossiers/{id}", dossierId)
                        .header("Authorization", "Bearer " + agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correction)))
                .andReturn();
        assertThat(nouvelleVersion.getResponse().getStatus()).isEqualTo(200);
        JsonNode corps = json(nouvelleVersion);
        assertThat(corps.path("id").asLong()).isNotEqualTo(dossierId);
        assertThat(corps.path("version").asInt()).isEqualTo(2);
        assertThat(corps.path("statut").asText()).isEqualTo("BROUILLON");
        assertThat(corps.path("documents")).isEmpty();
    }

    private long creerDossier(
            String token, long clientId, String numero, String revenu, String charges, String anciennete)
            throws Exception {
        Map<String, Object> demande = new LinkedHashMap<>();
        demande.put("clientId", clientId);
        demande.put("typePiece", "CNI");
        demande.put("numeroPiece", numero);
        demande.put("dateExpirationPiece", "2032-12-31");
        demande.put("revenuMensuel", revenu);
        demande.put("chargesMensuelles", charges);
        demande.put("ancienneteActiviteMois", anciennete);
        demande.put("nbPersonnesCharge", 2);

        MvcResult resultat = mockMvc.perform(post("/kyc/dossiers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(201);
        return json(resultat).path("id").asLong();
    }

    private void deposerPiece(String token, long dossierId) throws Exception {
        for (String type : new String[]{"RECTO_PIECE", "VERSO_PIECE"}) {
            MockMultipartFile fichier = new MockMultipartFile(
                    "fichier", type.toLowerCase() + ".png", "image/png", PNG);
            MvcResult resultat = mockMvc.perform(multipart("/kyc/dossiers/{id}/documents", dossierId)
                            .file(fichier)
                            .param("type", type)
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            assertThat(resultat.getResponse().getStatus()).isEqualTo(201);
        }
    }

    private void soumettre(String token, long dossierId) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/kyc/dossiers/{id}/soumission", dossierId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(resultat).path("statut").asText()).isEqualTo("SOUMIS");
    }

    private long clientId(String telephone) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM client WHERE telephone = ?", Long.class, telephone);
    }

    private long nouveauClient(String telephone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO client (agence_id, nom, prenom, telephone)
                SELECT id, 'NOUVEAU', 'Client', ?
                FROM agence WHERE code = 'LOME-CENTRE'
                RETURNING id
                """, Long.class, telephone);
    }
}
