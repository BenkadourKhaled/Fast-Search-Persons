package com.fastsearch.persons.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDatabaseInitializerTest {

    @Test
    void cree_les_tables_et_les_comptes_de_facon_idempotente() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:security-init;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        SecurityProperties properties = new SecurityProperties();
        properties.setDefaultUsers(false);
        properties.setUsers(List.of(new SecurityProperties.User(
                "admin@fastsearch.test", "Secret-123", List.of("ADMIN"), List.of("A", "B"))));

        ApplicationRunner runner = new SecurityDatabaseInitializer()
                .securityDatabaseRunner(jdbcTemplate, properties, encoder);
        runner.run(null);
        runner.run(null);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fastsearch_users", Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fastsearch_authorities", Integer.class));

        String password = jdbcTemplate.queryForObject(
                "SELECT password FROM fastsearch_users WHERE username = ?",
                String.class, "admin@fastsearch.test");
        assertTrue(encoder.matches("Secret-123", password),
                "Le mot de passe doit être stocké en BCrypt");

        List<String> authorities = jdbcTemplate.queryForList(
                "SELECT authority FROM fastsearch_authorities WHERE username = ? ORDER BY authority",
                String.class, "admin@fastsearch.test");
        assertEquals(List.of("GROUP_A", "GROUP_B", "ROLE_ADMIN"), authorities);
    }
}
