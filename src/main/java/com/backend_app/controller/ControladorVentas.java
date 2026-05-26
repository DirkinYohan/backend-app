package com.backend_app.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaVenta;
import com.backend_app.dto.SolicitudCrearVenta;
import com.backend_app.model.Rol;
import com.backend_app.service.ServicioVentas;
import com.backend_app.util.ExcepcionProhibido;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/sales")
public class ControladorVentas {
	private final ServicioVentas servicioVentas;

	public ControladorVentas(ServicioVentas servicioVentas) {
		this.servicioVentas = servicioVentas;
	}

	@PostMapping
	public ResponseEntity<RespuestaVenta> create(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudCrearVenta request) {
		return ResponseEntity.ok(servicioVentas.create(principal.storeId(), principal.userId(), request));
	}

	@GetMapping
	public ResponseEntity<List<RespuestaVenta>> list(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "mine", defaultValue = "false") boolean mine,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "from", required = false) Instant from,
			@RequestParam(name = "to", required = false) Instant to,
			@RequestParam(name = "operatorId", required = false) UUID operatorId,
			@RequestParam(name = "limit", defaultValue = "50") int limit) {
		if (principal.role() != Rol.ADMINISTRADOR) {
			return ResponseEntity.ok(servicioVentas.listForOperator(principal.storeId(), principal.userId(), q, from, to, limit));
		}
		if (mine) {
			return ResponseEntity.ok(servicioVentas.listForOperator(principal.storeId(), principal.userId(), limit));
		}
		return ResponseEntity.ok(servicioVentas.listForAdmin(principal.storeId(), q, from, to, operatorId, limit));
	}

	@GetMapping("/{saleId}")
	public ResponseEntity<RespuestaVenta> get(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID saleId) {
		RespuestaVenta response = servicioVentas.get(principal.storeId(), saleId);
		if (principal.role() == Rol.OPERADOR && !principal.userId().equals(response.operator().id())) {
			throw new ExcepcionProhibido("Acceso denegado");
		}
		return ResponseEntity.ok(response);
	}
}
