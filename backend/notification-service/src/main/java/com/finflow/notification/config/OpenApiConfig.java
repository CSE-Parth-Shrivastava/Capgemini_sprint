package com.finflow.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .version("1.0")
                        .description(
                            "Stores and delivers in-app and email notifications for all FinFlow workflow events.\n\n" +
                            "**Event types automatically fired by other services:**\n" +
                            "- `SIGNUP_SUCCESS` — fired by auth-service on user registration\n" +
                            "- `LOGIN_SUCCESS` — fired by auth-service on login\n" +
                            "- `APPLICATION_CREATED` — fired by application-service on DRAFT creation\n" +
                            "- `APPLICATION_SUBMITTED` — fired by application-service on submission\n" +
                            "- `DOCUMENT_UPLOADED` — fired by document-service on file upload\n" +
                            "- `DOCUMENT_VERIFIED` — fired by document-service on admin verification\n" +
                            "- `DOCUMENT_REJECTED` — fired by document-service on admin rejection\n" +
                            "- `APPLICATION_APPROVED` — fired by admin-service on loan approval\n" +
                            "- `APPLICATION_REJECTED` — fired by admin-service on loan rejection\n\n" +
                            "All notifications are persisted to the `finflow_notification` database."))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token returned by /auth/login or /auth/signup")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}