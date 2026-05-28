package com.backend_app.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.config.PropiedadesJwtSeguridad;
import com.backend_app.dto.RespuestaAutenticacion;
import com.backend_app.dto.RespuestaToken;
import com.backend_app.dto.SolicitudLogin;
import com.backend_app.dto.SolicitudRefresh;
import com.backend_app.dto.SolicitudRegistroAdmin;
import com.backend_app.model.Rol;
import com.backend_app.model.Tienda;
import com.backend_app.model.TokenRefresh;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioTiendas;
import com.backend_app.repository.RepositorioTokensRefresh;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.util.GeneradorRefreshToken;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoAutorizado;
import com.backend_app.util.MapeadorUsuario;
import com.backend_app.util.ServicioJwt;
import com.backend_app.util.UtilHashing;

/**
 * Servicio encargado de la lógica de autenticación y gestión de usuarios.
 * Maneja el registro de administradores, inicio de sesión, renovación de tokens y cierre de sesión.
 */
@Service
public class ServicioAutenticacion {
	private final RepositorioUsuarios userRepository;
	private final RepositorioTiendas storeRepository;
	private final RepositorioTokensRefresh refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final ServicioJwt servicioJwt;
	private final PropiedadesJwtSeguridad jwtProperties;

	public ServicioAutenticacion(RepositorioUsuarios userRepository, RepositorioTiendas storeRepository,
			RepositorioTokensRefresh refreshTokenRepository, PasswordEncoder passwordEncoder, ServicioJwt servicioJwt,
			PropiedadesJwtSeguridad jwtProperties) {
		this.userRepository = userRepository;
		this.storeRepository = storeRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.servicioJwt = servicioJwt;
		this.jwtProperties = jwtProperties;
	}

	/**
	 * Registra un nuevo administrador y crea una tienda asociada.
	 * 
	 * @param request Datos del registro.
	 * @return Respuesta con los tokens de acceso y la información del usuario.
	 * @throws ExcepcionConflicto Si el correo ya está registrado.
	 */
	@Transactional
	public RespuestaAutenticacion registerAdmin(SolicitudRegistroAdmin request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ExcepcionConflicto("El correo ya está registrado");
		}

		// Crear una nueva tienda para el administrador
		Tienda store = new Tienda();
		store = storeRepository.save(store);

		// Crear el usuario con rol de ADMINISTRADOR
		Usuario user = new Usuario();
		user.setStore(store);
		user.setRole(Rol.ADMINISTRADOR);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setIdentification(request.identification().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user = userRepository.save(user);

		// Vincular la tienda con el usuario administrador
		store.setAdminUserId(user.getId());
		storeRepository.save(store);

		return issueAuthResponse(user);
	}

	/**
	 * Autentica a un usuario y genera tokens de acceso.
	 * 
	 * @param request Credenciales de inicio de sesión.
	 * @return Respuesta con los tokens de acceso y la información del usuario.
	 * @throws ExcepcionNoAutorizado Si las credenciales son inválidas.
	 */
	@Transactional(readOnly = true)
	public RespuestaAutenticacion login(SolicitudLogin request) {
		String email = normalizeEmail(request.email());
		Usuario user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ExcepcionNoAutorizado(
				"Credenciales inválidas"));

		// Verificar que la contraseña coincida
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ExcepcionNoAutorizado("Credenciales inválidas");
		}

		return issueAuthResponse(user);
	}

	/**
	 * Renueva el token de acceso utilizando un refresh token válido.
	 * 
	 * @param request Contiene el refresh token actual.
	 * @return Nuevos tokens de acceso y de renovación.
	 * @throws ExcepcionNoAutorizado Si el refresh token es inválido, expirado o revocado.
	 */
	@Transactional
	public RespuestaToken refresh(SolicitudRefresh request) {
		String tokenHash = UtilHashing.sha256Hex(request.refreshToken());
		TokenRefresh stored = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new ExcepcionNoAutorizado("Refresh token inválido"));

		if (stored.getRevokedAt() != null) {
			throw new ExcepcionNoAutorizado("Refresh token revocado");
		}

		if (stored.getExpiresAt().isBefore(Instant.now())) {
			throw new ExcepcionNoAutorizado("Refresh token expirado");
		}

		// Revocar el token actual y generar uno nuevo (rotación de tokens)
		Usuario user = stored.getUser();
		stored.setRevokedAt(Instant.now());
		refreshTokenRepository.save(stored);

		String newRefreshToken = persistRefreshToken(user);
		String accessToken = servicioJwt.crearAccessToken(user.getId(), user.getStore().getId(), user.getRole(),
				user.getEmail());

		return new RespuestaToken(accessToken, newRefreshToken);
	}

	/**
	 * Cierra la sesión del usuario revocando su refresh token.
	 * 
	 * @param request Contiene el refresh token a revocar.
	 */
	@Transactional
	public void logout(SolicitudRefresh request) {
		String tokenHash = UtilHashing.sha256Hex(request.refreshToken());
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(stored -> {
			if (stored.getRevokedAt() == null) {
				stored.setRevokedAt(Instant.now());
				refreshTokenRepository.save(stored);
			}
		});
	}

	/**
	 * Genera una respuesta completa de autenticación con JWT y Refresh Token.
	 */
	private RespuestaAutenticacion issueAuthResponse(Usuario user) {
		String accessToken = servicioJwt.crearAccessToken(user.getId(), user.getStore().getId(), user.getRole(),
				user.getEmail());
		String refreshToken = persistRefreshToken(user);
		return new RespuestaAutenticacion(accessToken, refreshToken, MapeadorUsuario.aRespuestaUsuario(user));
	}

	/**
	 * Crea y guarda un nuevo refresh token en la base de datos.
	 */
	private String persistRefreshToken(Usuario user) {
		String raw = GeneradorRefreshToken.nuevoToken();
		String hash = UtilHashing.sha256Hex(raw);

		TokenRefresh token = new TokenRefresh();
		token.setUser(user);
		token.setTokenHash(hash);
		token.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenDays(), ChronoUnit.DAYS));
		refreshTokenRepository.save(token);

		return raw;
	}

	/**
	 * Normaliza el correo electrónico a minúsculas y elimina espacios.
	 */
	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
