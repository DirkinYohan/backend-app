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

@Validated
@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {
	private final ServicioAutenticacion servicioAutenticacion;

	public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion) {
		this.servicioAutenticacion = servicioAutenticacion;
	}

	@PostMapping("/register")
	public ResponseEntity<RespuestaAutenticacion> register(@Valid @RequestBody SolicitudRegistroAdmin request) {
		return ResponseEntity.ok(servicioAutenticacion.registerAdmin(request));
	}

	@PostMapping("/login")
	public ResponseEntity<RespuestaAutenticacion> login(@Valid @RequestBody SolicitudLogin request) {
		return ResponseEntity.ok(servicioAutenticacion.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<RespuestaToken> refresh(@Valid @RequestBody SolicitudRefresh request) {
		return ResponseEntity.ok(servicioAutenticacion.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody SolicitudRefresh request) {
		servicioAutenticacion.logout(request);
		return ResponseEntity.noContent().build();
	}
}
