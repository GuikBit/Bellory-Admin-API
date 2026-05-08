package org.exemplo.belloryadmin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class SwaggerConfig {

    private final BuildProperties buildProperties;

    public SwaggerConfig(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.orElse(null);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        String buildTime = buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString()
                : "N/A";

        return new OpenAPI()
                .info(new Info()
                        .title("Bellory Admin API")
                        .version(version)
                        .description("Painel administrativo da plataforma Bellory.\n\n**Build:** " + buildTime)
                        .contact(new Contact()
                                .name("Bellory")
                                .email("contato@bellory.com.br")));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("bellory-admin-api")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
