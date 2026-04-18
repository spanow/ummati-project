package orga.takwa.ummati.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI umatiOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Ummati API")
                        .description("""
                                API REST de la plateforme **Ummati** — Bénévolat & Gestion d'ONG.
                                
                                ## Authentification
                                La plupart des endpoints nécessitent un **JWT Bearer token**.
                                Obtenez-le via `POST /api/v1/auth/login` puis cliquez sur **Authorize**.
                                
                                ## Codes d'erreur communs
                                | Code | Description |
                                |------|-------------|
                                | 400  | Données invalides |
                                | 401  | Non authentifié |
                                | 403  | Accès refusé |
                                | 404  | Ressource introuvable |
                                | 409  | Conflit (doublon, règle métier) |
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe Ummati")
                                .email("dev@ummati.org"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://ummati.org")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Développement local"),
                        new Server().url("https://api.ummati.org").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Entrez votre JWT token (sans le préfixe 'Bearer ')")
                        )
                );
    }
}

