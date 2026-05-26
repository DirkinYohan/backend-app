package com.backend_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudActualizarOperador(
		@NotBlank @Size(max = 80) String firstName,
		@NotBlank @Size(max = 80) String lastName,
		@NotBlank @Size(max = 40) String identification,
		@NotBlank @Email @Size(max = 120) String email,
		@Size(min = 8, max = 72) String password) {
}
