package com.backend_app.dto;

import java.time.Instant;
import java.util.UUID;

public record RespuestaCategoria(UUID id, String name, String description, Instant createdAt) {
}
