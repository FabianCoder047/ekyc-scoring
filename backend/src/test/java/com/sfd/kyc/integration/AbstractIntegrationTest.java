package com.sfd.kyc.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Tests d'intégration sur PostgreSQL réel.
 *
 * <p>Le chemin nominal utilise Testcontainers. En l'absence de démon Docker
 * accessible, un PostgreSQL embarqué prend le relais afin de pouvoir exécuter
 * les tests sur un poste de développement. Le schéma et les migrations sont
 * identiques dans les deux cas.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final Path STOCKAGE;
    private static final Base DATABASE;

    static {
        try {
            STOCKAGE = Files.createTempDirectory("kyc-stockage-it-");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        DATABASE = Base.demarrer();
        log.info("Base de test : {}", DATABASE.description());
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::utilisateur);
        registry.add("spring.datasource.password", DATABASE::motDePasse);
        registry.add("spring.flyway.user", DATABASE::utilisateur);
        registry.add("spring.flyway.password", DATABASE::motDePasse);
        registry.add("app.stockage.chemin", STOCKAGE::toString);
        registry.add("app.jwt.secret", () -> Base64.getEncoder().encodeToString("secret-de-test-kyc-32-octets-min!!".getBytes()));
        registry.add("app.chiffrement.cle", () -> Base64.getEncoder().encodeToString("cle-de-chiffrement-test-32-oct..".getBytes()));
        registry.add("app.security.max-tentatives", () -> "5");
        registry.add("logging.level.com.sfd.kyc", () -> "INFO");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void nettoyerContexte() {
        // MockMvc ne conserve pas de session : rien à faire, mais le hook
        // documente que chaque test s'authentifie explicitement.
    }

    protected String login(String email, String motDePasse) throws Exception {
        MvcResult resultat = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", motDePasse))))
                .andReturn();
        return lireChamp(resultat.getResponse(), "accessToken");
    }

    protected String refresh(String email, String motDePasse) throws Exception {
        MvcResult resultat = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", motDePasse))))
                .andReturn();
        return lireChamp(resultat.getResponse(), "refreshToken");
    }

    protected JsonNode json(MvcResult resultat) throws Exception {
        return objectMapper.readTree(resultat.getResponse().getContentAsByteArray());
    }

    protected String lireChamp(MockHttpServletResponse response, String champ) throws Exception {
        return objectMapper.readTree(response.getContentAsByteArray()).path(champ).asText();
    }

    protected record Base(String url, String utilisateur, String motDePasse, Object ressource, String description) {

        static Base demarrer() {
            try {
                PostgreSQLContainer<?> conteneur = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("kyc")
                        .withUsername("test")
                        .withPassword("test");
                conteneur.start();
                return new Base(conteneur.getJdbcUrl(), conteneur.getUsername(),
                        conteneur.getPassword(), conteneur, "Testcontainers PostgreSQL 16");
            } catch (Throwable dockerIndisponible) {
                try {
                    EmbeddedPostgres postgres = EmbeddedPostgres.builder().setPort(0).start();
                    return new Base(
                            postgres.getJdbcUrl("postgres", "postgres"),
                            "postgres",
                            "postgres",
                            postgres,
                            "PostgreSQL embarqué (Docker indisponible)");
                } catch (IOException e) {
                    throw new IllegalStateException("Impossible de démarrer PostgreSQL pour les tests", e);
                }
            }
        }
    }
}
