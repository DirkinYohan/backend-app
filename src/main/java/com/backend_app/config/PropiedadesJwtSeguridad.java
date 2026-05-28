//guardar y validar la configuración JWT del sistema. qué datos se van a usar para firmar y verificar los tokens.
package com.backend_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record PropiedadesJwtSeguridad(@NotBlank String secret, @Min(1) long accessTokenMinutes, @Min(1) long refreshTokenDays) {
}
