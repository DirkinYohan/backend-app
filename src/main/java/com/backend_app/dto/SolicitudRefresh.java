package com.backend_app.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudRefresh(@NotBlank String refreshToken) {
}
