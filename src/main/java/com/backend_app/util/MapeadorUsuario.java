package com.backend_app.util;

import com.backend_app.dto.RespuestaUsuario;
import com.backend_app.model.Usuario;

public final class MapeadorUsuario {
	private MapeadorUsuario() {
	}

	public static RespuestaUsuario aRespuestaUsuario(Usuario user) {
		return new RespuestaUsuario(user.getId(), user.getStore().getId(), user.getRole(), user.getFirstName(),
				user.getLastName(), user.getIdentification(), user.getEmail(), user.isActive());
	}
}
