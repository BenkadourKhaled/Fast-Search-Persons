package com.fastsearch.persons.security;

import com.fastsearch.persons.config.FastSearchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sécurité on-premise : HTTP Basic sans session, rôles ADMIN/ANALYST/READER
 * et autorités GROUP_xxx. Le filtre de groupes est appliqué dans la recherche,
 * pas seulement à l'URL.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "fastsearch.security", name = "store",
            havingValue = "memory", matchIfMissing = true)
    public UserDetailsService inMemoryUserDetailsService(SecurityProperties properties,
                                                         PasswordEncoder encoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        for (SecurityProperties.User user : properties.effectiveUsers()) {
            if (user.getUsername() == null || user.getUsername().isBlank()
                    || user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException("Utilisateur sans nom ou sans mot de passe configuré");
            }
            List<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : user.getRoles()) {
                String normalized = role.trim().toUpperCase(Locale.ROOT);
                if (!normalized.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
                }
            }
            for (String group : user.getGroups()) {
                String normalized = group.trim();
                if (!normalized.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("GROUP_" + normalized));
                }
            }
            UserDetails details = User.withUsername(user.getUsername())
                    .password(encoder.encode(user.getPassword()))
                    .authorities(authorities)
                    .build();
            manager.createUser(details);
        }
        return manager;
    }

    /**
     * Utilisateurs persistés dans PostgreSQL. Les tables sont créées et
     * alimentées au premier démarrage par {@link SecurityDatabaseInitializer}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "fastsearch.security", name = "store", havingValue = "jdbc")
    public UserDetailsService jdbcUserDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery(
                "SELECT username, password, enabled FROM fastsearch_users WHERE username = ?");
        manager.setAuthoritiesByUsernameQuery(
                "SELECT username, authority FROM fastsearch_authorities WHERE username = ?");
        return manager;
    }

    /**
     * CORS pour l'interface web. Les origines sont explicites et configurables
     * via {@code fastsearch.cors.allowed-origins}. Le préflight est traité
     * avant l'authentification par Spring Security.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(FastSearchProperties properties) {
        List<String> origins = properties.getCors().getAllowedOrigins().stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept",
                "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("WWW-Authenticate"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/error").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/eval/**").hasAnyRole("ADMIN", "ANALYST")
                        .requestMatchers("/api/v1/persons/**").hasAnyRole("ADMIN", "ANALYST", "READER")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
