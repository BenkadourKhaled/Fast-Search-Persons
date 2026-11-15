package com.fastsearch.persons.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI de l'API REST v1.
 *
 * <p>L'interface Swagger UI est exposée sur {@code /swagger-ui.html} et le
 * document JSON sur {@code /v3/api-docs}. Les opérations métier restent
 * protégées par HTTP Basic ; Swagger UI permet de saisir les identifiants.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fastSearchOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FastSearch Persons API")
                        .version("0.1.0")
                        .description("""
                                API REST de rapprochement de personnes on-premise.

                                Les résultats sont des candidats avec un score, un niveau de
                                confiance et une explication par champ. Aucune réponse ne constitue
                                une identification automatique. Les données de démonstration sont
                                entièrement synthétiques.
                                """)
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic — rôles ADMIN, ANALYST ou READER")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
