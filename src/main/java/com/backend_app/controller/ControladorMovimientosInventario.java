//Controlador para manejar las solicitudes de movimientos de inventario.
package com.backend_app.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaMovimientoInventario;
import com.backend_app.dto.SolicitudCrearMovimientoInventario;
import com.backend_app.service.ServicioMovimientosInventario;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/inventory/movements")
public class ControladorMovimientosInventario {
	private final ServicioMovimientosInventario servicioMovimientos;

	public ControladorMovimientosInventario(ServicioMovimientosInventario servicioMovimientos) {
		this.servicioMovimientos = servicioMovimientos;
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping
	public ResponseEntity<RespuestaMovimientoInventario> crear(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudCrearMovimientoInventario request) {
		return ResponseEntity.ok(servicioMovimientos.crear(principal.storeId(), principal.userId(), request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@GetMapping
	public ResponseEntity<List<RespuestaMovimientoInventario>> listar(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "from", required = false) Instant from,
			@RequestParam(name = "to", required = false) Instant to,
			@RequestParam(name = "limit", defaultValue = "50") int limit) {
		return ResponseEntity.ok(servicioMovimientos.listar(principal.storeId(), from, to, limit));
	}
}
