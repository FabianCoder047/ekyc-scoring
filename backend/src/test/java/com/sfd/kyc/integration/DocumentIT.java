package com.sfd.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class DocumentIT extends AbstractIntegrationTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2nWQAAAAASUVORK5CYII=");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void un_faux_jpeg_et_un_fichier_trop_gros_sont_rejetes() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long dossierId = dossierBrouillon(agent);

        byte[] exe = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MvcResult fauxJpeg = mockMvc.perform(multipart("/kyc/dossiers/{id}/documents", dossierId)
                        .file(new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", exe))
                        .param("type", "RECTO_PIECE")
                        .header("Authorization", "Bearer " + agent))
                .andReturn();
        assertThat(fauxJpeg.getResponse().getStatus()).isEqualTo(415);
        assertThat(json(fauxJpeg).path("code").asText()).isEqualTo("TYPE_DOCUMENT_NON_SUPPORTE");

        byte[] tropGros = new byte[6 * 1024 * 1024];
        MvcResult taille = mockMvc.perform(multipart("/kyc/dossiers/{id}/documents", dossierId)
                        .file(new MockMultipartFile("fichier", "gros.png", "image/png", tropGros))
                        .param("type", "RECTO_PIECE")
                        .header("Authorization", "Bearer " + agent))
                .andReturn();
        assertThat(taille.getResponse().getStatus()).isEqualTo(413);
        assertThat(json(taille).path("code").asText()).isEqualTo("FICHIER_TROP_VOLUMINEUX");
    }

    @Test
    void le_fichier_est_chiffre_et_le_telechargement_controle_est_fidele() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long dossierId = dossierBrouillon(agent);
        long documentId = deposer(agent, dossierId, "RECTO_PIECE", PNG);
        deposer(agent, dossierId, "VERSO_PIECE", PNG);

        String cle = jdbcTemplate.queryForObject(
                "SELECT cle_stockage FROM document_kyc WHERE id = ?", String.class, documentId);
        Path fichierStocke = STOCKAGE.resolve(cle + ".bin");
        byte[] stocke = Files.readAllBytes(fichierStocke);
        assertThat(Arrays.equals(Arrays.copyOf(stocke, 4), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}))
                .isFalse();
        assertThat(new String(stocke, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("%PDF");
        assertThat(Arrays.equals(stocke, PNG)).isFalse();

        soumettre(agent, dossierId);

        String analyste = login("analyste@sfd.tg", "Demo123456!");
        MvcResult avantAssignation = mockMvc.perform(
                        get("/kyc/dossiers/{id}/documents/{docId}/contenu", dossierId, documentId)
                                .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(avantAssignation.getResponse().getStatus()).isEqualTo(403);

        mockMvc.perform(post("/kyc/dossiers/{id}/prise-en-charge", dossierId)
                        .header("Authorization", "Bearer " + analyste))
                .andReturn();

        MvcResult telechargement = mockMvc.perform(
                        get("/kyc/dossiers/{id}/documents/{docId}/contenu", dossierId, documentId)
                                .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(telechargement.getResponse().getStatus()).isEqualTo(200);
        assertThat(telechargement.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(telechargement.getResponse().getContentAsByteArray()).isEqualTo(PNG);
        assertThat(telechargement.getResponse().getHeader("Cache-Control")).contains("no-store");

        Integer consultations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action = 'DOCUMENT_CONSULTE' AND entite_id = ?",
                Integer.class, documentId);
        assertThat(consultations).isEqualTo(1);

        byte[] altere = new byte[stocke.length];
        Arrays.fill(altere, (byte) 0x42);
        Files.write(fichierStocke, altere);

        MvcResult detection = mockMvc.perform(
                        get("/kyc/dossiers/{id}/documents/{docId}/contenu", dossierId, documentId)
                                .header("Authorization", "Bearer " + analyste))
                .andReturn();
        assertThat(detection.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(detection).path("code").asText()).isEqualTo("DOCUMENT_ALTERE");
    }

    @Test
    void un_agent_ne_peut_pas_consulter_le_contenu_d_un_document() throws Exception {
        String agent = login("agent@sfd.tg", "Demo123456!");
        long dossierId = dossierBrouillon(agent);
        long documentId = deposer(agent, dossierId, "RECTO_PIECE", PNG);

        MvcResult resultat = mockMvc.perform(
                        get("/kyc/dossiers/{id}/documents/{docId}/contenu", dossierId, documentId)
                                .header("Authorization", "Bearer " + agent))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(403);
    }

    private long dossierBrouillon(String token) throws Exception {
        long clientId = jdbcTemplate.queryForObject(
                "SELECT id FROM client WHERE telephone = '+22890000002'", Long.class);
        Map<String, Object> demande = new LinkedHashMap<>();
        demande.put("clientId", clientId);
        demande.put("typePiece", "CNI");
        demande.put("numeroPiece", "TG" + System.nanoTime());
        demande.put("dateExpirationPiece", "2032-12-31");
        demande.put("revenuMensuel", 250000);
        demande.put("chargesMensuelles", 60000);
        demande.put("ancienneteActiviteMois", 24);

        MvcResult resultat = mockMvc.perform(post("/kyc/dossiers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demande)))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(201);
        JsonNode corps = json(resultat);
        return corps.path("id").asLong();
    }

    private long deposer(String token, long dossierId, String type, byte[] contenu) throws Exception {
        MvcResult resultat = mockMvc.perform(multipart("/kyc/dossiers/{id}/documents", dossierId)
                        .file(new MockMultipartFile("fichier", "piece.png", "image/png", contenu))
                        .param("type", type)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(201);
        return json(resultat).path("id").asLong();
    }

    private void soumettre(String token, long dossierId) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/kyc/dossiers/{id}/soumission", dossierId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(resultat.getResponse().getStatus()).isEqualTo(200);
    }
}
