package com.backend_app.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaUsuario;
import com.backend_app.dto.SolicitudActualizacionPerfil;
import com.backend_app.dto.SolicitudCambioContrasena;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoAutorizado;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.MapeadorUsuario;

@Service
public class ServicioPerfil {
	private final RepositorioUsuarios userRepository;
	private final PasswordEncoder passwordEncoder;

	public ServicioPerfil(RepositorioUsuarios userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public RespuestaUsuario updateProfile(UUID storeId, UUID userId, SolicitudActualizacionPerfil request) {
		Usuario user = userRepository.findByIdAndStore_Id(userId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));

		String email = normalizeEmail(request.email());
		if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
			throw new ExcepcionConflicto("El correo ya está registrado");
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setIdentification(request.identification().trim());
		user.setEmail(email);
		user = userRepository.save(user);

		return MapeadorUsuario.aRespuestaUsuario(user);
	}

	@Transactional
	public void changePassword(UUID storeId, UUID userId, SolicitudCambioContrasena request) {
		Usuario user = userRepository.findByIdAndStore_Id(userId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ExcepcionNoAutorizado("Contraseña actual inválida");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
