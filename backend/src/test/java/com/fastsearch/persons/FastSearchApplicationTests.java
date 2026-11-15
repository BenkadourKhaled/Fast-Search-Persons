package com.fastsearch.persons;

import com.fastsearch.persons.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Path;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FastSearchApplicationTests {

    private static final String ADMIN_USERNAME = "benkkadour.khaled@gmail.com";
    private static final String ADMIN_PASSWORD = "KHALED123";
    private static final String ANALYST_USERNAME = "amina.benali@fastsearch.test";
    private static final String ANALYST_PASSWORD = "Amina-Analyste-2026";
    private static final String READER_USERNAME = "sara.haddad@fastsearch.test";
    private static final String READER_PASSWORD = "Sara-Lecteur-2026";

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:fastsearch-app;"
                + "DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("fastsearch.index.path", () -> tempDir.resolve("index").toString());
        registry.add("fastsearch.demo.enabled", () -> "true");
        registry.add("fastsearch.eval.count", () -> "12");
        registry.add("fastsearch.security.default-users", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void sante_publique_sans_authentification() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void identite_effective_pour_le_front() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
                .andExpect(jsonPath("$.groups", hasItem("A")))
                .andExpect(jsonPath("$.groups", hasItem("B")));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cors_preflight_pour_interface_vite() throws Exception {
        mockMvc.perform(options("/api/v1/persons/search")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "http://localhost:5173"));
    }

    @Test
    void recherche_authentifiee_et_groupes() throws Exception {
        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "Walid")
                        .with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits").isNotEmpty())
                .andExpect(jsonPath("$.hits[0].ownerGroup").isString())
                .andExpect(jsonPath("$.hits[0].explanations").isArray())
                .andExpect(jsonPath("$.hits[*].id", not(hasItem("D005"))));

        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "Protégé Secret")
                        .with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits").isEmpty());

        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "Protégé Secret")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits[*].id", hasItem("D005")));
    }

    @Test
    void recherche_arabe_sur_les_noms_inventes() throws Exception {
        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "بوزيدي")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits").isNotEmpty())
                .andExpect(jsonPath("$.hits[*].id", hasItem("D049")));
    }

    @Test
    void reader_ne_peut_pas_reindexer() throws Exception {
        mockMvc.perform(post("/api/v1/admin/index/reindex")
                        .with(reader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyste_peut_lancer_evaluation() throws Exception {
        mockMvc.perform(post("/api/v1/eval/run")
                        .param("count", "12")
                        .with(analyst()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personCount").value(15))
                .andExpect(jsonPath("$.caseCount").value(75))
                .andExpect(jsonPath("$.lucene.recallAt10").isNumber())
                .andExpect(jsonPath("$.baselines.length()").value(2));
    }

    @Test
    void admin_consulte_index_et_audit() throws Exception {
        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "Walid")
                        .with(reader()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/index/stats")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numDocs").value(196))
                .andExpect(jsonPath("$.open").value(true));

        mockMvc.perform(get("/api/v1/admin/audit")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].queryHash").isString());

        long count = auditEventRepository.count();
        org.junit.jupiter.api.Assertions.assertTrue(count > 0,
                "La recherche doit produire un événement d'audit");
    }

    @Test
    void audit_sans_requete_brute() throws Exception {
        mockMvc.perform(get("/api/v1/persons/search")
                        .param("q", "Walid")
                        .with(reader()))
                .andExpect(status().isOk());
        auditEventRepository.findAll().forEach(event -> {
            org.junit.jupiter.api.Assertions.assertFalse(event.getQueryHash().contains("Walid"));
            org.junit.jupiter.api.Assertions.assertFalse(event.getDetails() != null
                    && event.getDetails().contains("Walid"));
        });
    }

    @Test
    void validation_et_authentification() throws Exception {
        mockMvc.perform(get("/api/v1/persons/search").param("q", "Walid"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/persons/search")
                        .with(reader())
                        .contentType("application/json")
                        .content("{\"q\":\"" + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor admin() {
        return httpBasic(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private static RequestPostProcessor analyst() {
        return httpBasic(ANALYST_USERNAME, ANALYST_PASSWORD);
    }

    private static RequestPostProcessor reader() {
        return httpBasic(READER_USERNAME, READER_PASSWORD);
    }
}
