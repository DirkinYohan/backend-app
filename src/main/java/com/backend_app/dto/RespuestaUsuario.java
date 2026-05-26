package com.backend_app.dto;

import java.util.UUID;

import com.backend_app.model.Rol;

public record RespuestaUsuario(UUID id, UUID storeId, Rol role, String firstName, String lastName, String identification,
		String email, boolean active) {
}
