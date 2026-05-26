package com.backend_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "security.cors")
public record PropiedadesCors(@NotBlank String allowedOrigins) {
}
