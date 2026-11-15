package com.fastsearch.persons.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Initialise le stock d'identités PostgreSQL lorsque
 * {@code fastsearch.security.store=jdbc}.
 *
 * <p>Les tables sont créées si nécessaire, puis les utilisateurs de la
 * configuration sont insérés de façon idempotente. Un compte déjà présent
 * n'est jamais écrasé : son mot de passe et son état restent inchangés, seules
 * les autorités manquantes sont ajoutées. Cela permet de redémarrer
 * l'application sans réinitialiser les comptes gérés en base.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "fastsearch.security", name = "store", havingValue = "jdbc")
public class SecurityDatabaseInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityDatabaseInitializer.class);

    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS fastsearch_users (
                username VARCHAR(255) PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT TRUE
            )
            """;

    private static final String CREATE_AUTHORITIES = """
            CREATE TABLE IF NOT EXISTS fastsearch_authorities (
                username VARCHAR(255) NOT NULL,
                authority VARCHAR(255) NOT NULL,
                PRIMARY KEY (username, authority),
                CONSTRAINT fk_fastsearch_authorities_user
                    FOREIGN KEY (username) REFERENCES fastsearch_users (username)
                    ON DELETE CASCADE
            )
            """;

    @Bean
    public ApplicationRunner securityDatabaseRunner(JdbcTemplate jdbcTemplate,
                                                    SecurityProperties properties,
                                                    PasswordEncoder encoder) {
        return args -> {
            jdbcTemplate.execute(CREATE_USERS);
            jdbcTemplate.execute(CREATE_AUTHORITIES);

            int createdUsers = 0;
            int addedAuthorities = 0;
            for (SecurityProperties.User user : properties.effectiveUsers()) {
                String username = user.getUsername();
                if (username == null || username.isBlank()
                        || user.getPassword() == null || user.getPassword().isBlank()) {
                    throw new IllegalStateException(
                            "Utilisateur sans nom ou sans mot de passe configuré");
                }

                Integer existing = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM fastsearch_users WHERE username = ?",
                        Integer.class, username);
                if (existing == null || existing == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO fastsearch_users (username, password, enabled) VALUES (?, ?, TRUE)",
                            username, encoder.encode(user.getPassword()));
                    createdUsers++;
                }

                for (String authority : authorities(user)) {
                    Integer present = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM fastsearch_authorities "
                                    + "WHERE username = ? AND authority = ?",
                            Integer.class, username, authority);
                    if (present == null || present == 0) {
                        jdbcTemplate.update(
                                "INSERT INTO fastsearch_authorities (username, authority) VALUES (?, ?)",
                                username, authority);
                        addedAuthorities++;
                    }
                }
            }
            LOG.info("Sécurité PostgreSQL : {} utilisateur(s) créé(s), {} autorité(s) ajoutée(s)",
                    createdUsers, addedAuthorities);
        };
    }

    static List<String> authorities(SecurityProperties.User user) {
        List<String> authorities = new ArrayList<>();
        for (String role : user.getRoles()) {
            String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                authorities.add("ROLE_" + normalized);
            }
        }
        for (String group : user.getGroups()) {
            String normalized = group == null ? "" : group.trim();
            if (!normalized.isBlank()) {
                authorities.add("GROUP_" + normalized);
            }
        }
        return authorities;
    }
}
