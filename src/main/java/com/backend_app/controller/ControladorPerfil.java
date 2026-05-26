package com.backend_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaUsuario;
import com.backend_app.dto.SolicitudActualizacionPerfil;
import com.backend_app.dto.SolicitudCambioContrasena;
import com.backend_app.service.ServicioPerfil;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/profile")
public class ControladorPerfil {
	private final ServicioPerfil servicioPerfil;

	public ControladorPerfil(ServicioPerfil servicioPerfil) {
		this.servicioPerfil = servicioPerfil;
	}

	@PutMapping
	public ResponseEntity<RespuestaUsuario> update(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudActualizacionPerfil request) {
		return ResponseEntity.ok(servicioPerfil.updateProfile(principal.storeId(), principal.userId(), request));
	}

	@PostMapping("/password")
	public ResponseEntity<Void> changePassword(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudCambioContrasena request) {
		servicioPerfil.changePassword(principal.storeId(), principal.userId(), request);
		return ResponseEntity.noContent().build();
	}
}
