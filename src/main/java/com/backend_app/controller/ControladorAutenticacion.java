package com.backend_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.dto.RespuestaAutenticacion;
import com.backend_app.dto.RespuestaToken;
import com.backend_app.dto.SolicitudLogin;
import com.backend_app.dto.SolicitudRefresh;
import com.backend_app.dto.SolicitudRegistroAdmin;
import com.backend_app.service.ServicioAutenticacion;

import jakarta.validation.Valid;

/**
 * Controlador REST para manejar las operaciones de autenticación.
 * Proporciona endpoints para registro, inicio de sesión, renovación de tokens y cierre de sesión.
 */
@Validated
@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {
	private final ServicioAutenticacion servicioAutenticacion;

	public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion) {
		this.servicioAutenticacion = servicioAutenticacion;
	}

	/**
	 * Endpoint para registrar un nuevo administrador.
	 * 
	 * @param request Datos del registro validados.
	 * @return ResponseEntity con la respuesta de autenticación.
	 */
	@PostMapping("/register")
	public ResponseEntity<RespuestaAutenticacion> register(@Valid @RequestBody SolicitudRegistroAdmin request) {
		return ResponseEntity.ok(servicioAutenticacion.registerAdmin(request));
	}

	/**
	 * Endpoint para iniciar sesión.
	 * 
	 * @param request Credenciales de login validadas.
	 * @return ResponseEntity con la respuesta de autenticación.
	 */
	@PostMapping("/login")
	public ResponseEntity<RespuestaAutenticacion> login(@Valid @RequestBody SolicitudLogin request) {
		return ResponseEntity.ok(servicioAutenticacion.login(request));
	}

	/**
	 * Endpoint para renovar el token de acceso.
	 * 
	 * @param request Datos del refresh token validados.
	 * @return ResponseEntity con el nuevo token.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<RespuestaToken> refresh(@Valid @RequestBody SolicitudRefresh request) {
		return ResponseEntity.ok(servicioAutenticacion.refresh(request));
	}

	/**
	 * Endpoint para cerrar sesión y revocar el refresh token.
	 * 
	 * @param request Datos del refresh token a invalidar.
	 * @return ResponseEntity sin contenido (204 No Content).
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody SolicitudRefresh request) {
		servicioAutenticacion.logout(request);
		return ResponseEntity.noContent().build();
	}
}
