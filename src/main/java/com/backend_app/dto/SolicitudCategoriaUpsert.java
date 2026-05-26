package com.backend_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCategoriaUpsert(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 500) String description) {
}
