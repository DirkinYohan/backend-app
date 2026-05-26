package com.backend_app.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaUsuario;
import com.backend_app.dto.SolicitudActualizarOperador;
import com.backend_app.dto.SolicitudCrearOperador;
import com.backend_app.service.ServicioOperadores;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api")
public class ControladorUsuarios {
	private final ServicioOperadores servicioOperadores;

	public ControladorUsuarios(ServicioOperadores servicioOperadores) {
		this.servicioOperadores = servicioOperadores;
	}

	@GetMapping("/me")
	public ResponseEntity<RespuestaUsuario> yo(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal) {
		return ResponseEntity.ok(servicioOperadores.obtenerMiPerfil(principal.userId()));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/users/operators")
	public ResponseEntity<RespuestaUsuario> crearOperador(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudCrearOperador request) {
		return ResponseEntity.ok(servicioOperadores.crearOperador(principal.storeId(), request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@GetMapping("/users/operators")
	public ResponseEntity<List<RespuestaUsuario>> listarOperadores(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal) {
		return ResponseEntity.ok(servicioOperadores.listarOperadores(principal.storeId()));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PutMapping("/users/operators/{operatorId}")
	public ResponseEntity<RespuestaUsuario> actualizarOperador(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@PathVariable UUID operatorId, @Valid @RequestBody SolicitudActualizarOperador request) {
		return ResponseEntity.ok(servicioOperadores.actualizarOperador(principal.storeId(), operatorId, request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/users/operators/{operatorId}/active")
	public ResponseEntity<RespuestaUsuario> cambiarActivoOperador(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@PathVariable UUID operatorId, @RequestParam("value") boolean value) {
		return ResponseEntity.ok(servicioOperadores.cambiarActivoOperador(principal.storeId(), operatorId, value));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@DeleteMapping("/users/operators/{operatorId}")
	public ResponseEntity<Void> eliminarOperador(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID operatorId) {
		servicioOperadores.cambiarActivoOperador(principal.storeId(), operatorId, false);
		return ResponseEntity.noContent().build();
	}
}
