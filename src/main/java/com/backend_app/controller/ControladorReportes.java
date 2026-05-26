package com.backend_app.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaReporte;
import com.backend_app.service.ServicioReportes;

@Validated
@RestController
@RequestMapping("/api/reports")
public class ControladorReportes {
	private final ServicioReportes servicioReportes;

	public ControladorReportes(ServicioReportes servicioReportes) {
		this.servicioReportes = servicioReportes;
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@GetMapping
	public ResponseEntity<RespuestaReporte> reporte(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "from", required = false) Instant from,
			@RequestParam(name = "to", required = false) Instant to,
			@RequestParam(name = "operatorId", required = false) UUID operatorId) {
		return ResponseEntity.ok(servicioReportes.generarReporte(principal.storeId(), from, to, operatorId));
	}
}
