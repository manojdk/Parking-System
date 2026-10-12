package com.parkingSystem.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * OpenAPI metadata. Swagger UI is served at /swagger-ui.html; use the "Authorize" button
 * with a token from POST /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Parking System API", version = "0.0.1",
                description = "REST API for parking spaces, vehicle entry/exit, billing, reservations and feedback"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
