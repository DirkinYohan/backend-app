package com.backend_app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaUsuario;
import com.backend_app.dto.SolicitudActualizarOperador;
import com.backend_app.dto.SolicitudCrearOperador;
import com.backend_app.model.Rol;
import com.backend_app.model.Tienda;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioTiendas;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.util.MapeadorUsuario;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoEncontrado;

@Service
public class ServicioOperadores {
	private final RepositorioUsuarios repositorioUsuarios;
	private final RepositorioTiendas repositorioTiendas;
	private final PasswordEncoder codificadorContrasenas;

	public ServicioOperadores(RepositorioUsuarios repositorioUsuarios, RepositorioTiendas repositorioTiendas,
			PasswordEncoder codificadorContrasenas) {
		this.repositorioUsuarios = repositorioUsuarios;
		this.repositorioTiendas = repositorioTiendas;
		this.codificadorContrasenas = codificadorContrasenas;
	}

	@Transactional
	public RespuestaUsuario crearOperador(UUID storeId, SolicitudCrearOperador request) {
		String email = normalizarEmail(request.email());
		if (repositorioUsuarios.existsByEmailIgnoreCase(email)) {
			throw new ExcepcionConflicto("El correo ya está registrado");
		}

		Tienda store = repositorioTiendas.findById(storeId).orElseThrow(() -> new ExcepcionNoEncontrado("Tienda no encontrada"));

		Usuario user = new Usuario();
		user.setStore(store);
		user.setRole(Rol.OPERADOR);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setIdentification(request.identification().trim());
		user.setEmail(email);
		user.setPasswordHash(codificadorContrasenas.encode(request.password()));
		user = repositorioUsuarios.save(user);

		return MapeadorUsuario.aRespuestaUsuario(user);
	}

	@Transactional(readOnly = true)
	public List<RespuestaUsuario> listarOperadores(UUID storeId) {
		return repositorioUsuarios.findAllByStore_IdAndRoleOrderByCreatedAtDesc(storeId, Rol.OPERADOR).stream()
				.map(MapeadorUsuario::aRespuestaUsuario).toList();
	}

	@Transactional
	public RespuestaUsuario actualizarOperador(UUID storeId, UUID operatorId, SolicitudActualizarOperador request) {
		Usuario user = repositorioUsuarios.findByIdAndStore_Id(operatorId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Operador no encontrado"));
		if (user.getRole() != Rol.OPERADOR) {
			throw new ExcepcionNoEncontrado("Operador no encontrado");
		}

		String email = normalizarEmail(request.email());
		if (!email.equalsIgnoreCase(user.getEmail()) && repositorioUsuarios.existsByEmailIgnoreCase(email)) {
			throw new ExcepcionConflicto("El correo ya está registrado");
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setIdentification(request.identification().trim());
		user.setEmail(email);
		if (request.password() != null && !request.password().isBlank()) {
			user.setPasswordHash(codificadorContrasenas.encode(request.password()));
		}

		user = repositorioUsuarios.save(user);
		return MapeadorUsuario.aRespuestaUsuario(user);
	}

	@Transactional
	public RespuestaUsuario cambiarActivoOperador(UUID storeId, UUID operatorId, boolean active) {
		Usuario user = repositorioUsuarios.findByIdAndStore_Id(operatorId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Operador no encontrado"));
		if (user.getRole() != Rol.OPERADOR) {
			throw new ExcepcionNoEncontrado("Operador no encontrado");
		}
		user.setActive(active);
		user = repositorioUsuarios.save(user);
		return MapeadorUsuario.aRespuestaUsuario(user);
	}

	@Transactional(readOnly = true)
	public RespuestaUsuario obtenerMiPerfil(UUID userId) {
		Usuario user = repositorioUsuarios.findById(userId).orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
		return MapeadorUsuario.aRespuestaUsuario(user);
	}

	private static String normalizarEmail(String email) {
		return email.trim().toLowerCase();
	}
}
