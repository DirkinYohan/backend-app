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
import com.backend_app.dto.RespuestaProducto;
import com.backend_app.dto.SolicitudActualizarStockProducto;
import com.backend_app.dto.SolicitudProductoUpsert;
import com.backend_app.service.ServicioProductos;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/products")
public class ControladorProductos {
	private final ServicioProductos servicioProductos;

	public ControladorProductos(ServicioProductos servicioProductos) {
		this.servicioProductos = servicioProductos;
	}

	@GetMapping
	public ResponseEntity<List<RespuestaProducto>> search(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "active", required = false) Boolean active,
			@RequestParam(name = "categoryId", required = false) UUID categoryId) {
		return ResponseEntity.ok(servicioProductos.search(principal.storeId(), q, active, categoryId));
	}

	@GetMapping("/low-stock")
	public ResponseEntity<List<RespuestaProducto>> lowStock(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "limit", defaultValue = "10") int limit) {
		return ResponseEntity.ok(servicioProductos.lowStock(principal.storeId(), limit));
	}

	@GetMapping("/{productId}")
	public ResponseEntity<RespuestaProducto> get(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId) {
		return ResponseEntity.ok(servicioProductos.get(principal.storeId(), productId));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping
	public ResponseEntity<RespuestaProducto> create(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudProductoUpsert request) {
		return ResponseEntity.ok(servicioProductos.create(principal.storeId(), request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PutMapping("/{productId}")
	public ResponseEntity<RespuestaProducto> update(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@Valid @RequestBody SolicitudProductoUpsert request) {
		return ResponseEntity.ok(servicioProductos.update(principal.storeId(), productId, request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/{productId}/active")
	public ResponseEntity<RespuestaProducto> setActive(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@RequestParam("value") boolean value) {
		return ResponseEntity.ok(servicioProductos.setActive(principal.storeId(), productId, value));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/{productId}/stock")
	public ResponseEntity<RespuestaProducto> updateStock(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@Valid @RequestBody SolicitudActualizarStockProducto request) {
		return ResponseEntity.ok(servicioProductos.updateStock(principal.storeId(), principal.userId(), productId, request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId) {
		servicioProductos.delete(principal.storeId(), productId);
		return ResponseEntity.noContent().build();
	}
}
