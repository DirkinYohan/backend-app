package com.backend_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCambioContrasena(
		@NotBlank @Size(min = 8, max = 72) String currentPassword,
		@NotBlank @Size(min = 8, max = 72) String newPassword) {
}
