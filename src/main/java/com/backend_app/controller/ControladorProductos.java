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

/**
 * Controlador REST para la gestión de productos.
 * Proporciona endpoints para buscar, crear, actualizar, eliminar y gestionar el stock de productos.
 */
@Validated
@RestController
@RequestMapping("/api/products")
public class ControladorProductos {
	private final ServicioProductos servicioProductos;

	public ControladorProductos(ServicioProductos servicioProductos) {
		this.servicioProductos = servicioProductos;
	}

	/**
	 * Busca productos según filtros opcionales.
	 * 
	 * @param principal Usuario autenticado.
	 * @param q Término de búsqueda (nombre o código).
	 * @param active Filtrar por estado activo/inactivo.
	 * @param categoryId Filtrar por categoría.
	 * @return Lista de productos que coinciden con los criterios.
	 */
	@GetMapping
	public ResponseEntity<List<RespuestaProducto>> search(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "active", required = false) Boolean active,
			@RequestParam(name = "categoryId", required = false) UUID categoryId) {
		return ResponseEntity.ok(servicioProductos.search(principal.storeId(), q, active, categoryId));
	}

	/**
	 * Obtiene productos con bajo stock.
	 */
	@GetMapping("/low-stock")
	public ResponseEntity<List<RespuestaProducto>> lowStock(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "limit", defaultValue = "10") int limit) {
		return ResponseEntity.ok(servicioProductos.lowStock(principal.storeId(), limit));
	}

	/**
	 * Obtiene el detalle de un producto por su ID.
	 */
	@GetMapping("/{productId}")
	public ResponseEntity<RespuestaProducto> get(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId) {
		return ResponseEntity.ok(servicioProductos.get(principal.storeId(), productId));
	}

	/**
	 * Crea un nuevo producto. Solo accesible por administradores.
	 */
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping
	public ResponseEntity<RespuestaProducto> create(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudProductoUpsert request) {
		return ResponseEntity.ok(servicioProductos.create(principal.storeId(), request));
	}

	/**
	 * Actualiza un producto existente. Solo accesible por administradores.
	 */
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PutMapping("/{productId}")
	public ResponseEntity<RespuestaProducto> update(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@Valid @RequestBody SolicitudProductoUpsert request) {
		return ResponseEntity.ok(servicioProductos.update(principal.storeId(), productId, request));
	}

	/**
	 * Activa o desactiva un producto. Solo accesible por administradores.
	 */
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/{productId}/active")
	public ResponseEntity<RespuestaProducto> setActive(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@RequestParam("value") boolean value) {
		return ResponseEntity.ok(servicioProductos.setActive(principal.storeId(), productId, value));
	}

	/**
	 * Actualiza manualmente el stock de un producto. Solo accesible por administradores.
	 */
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/{productId}/stock")
	public ResponseEntity<RespuestaProducto> updateStock(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@Valid @RequestBody SolicitudActualizarStockProducto request) {
		return ResponseEntity.ok(servicioProductos.updateStock(principal.storeId(), principal.userId(), productId, request));
	}

	/**
	 * Elimina un producto. Solo accesible por administradores.
	 */
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId) {
		servicioProductos.delete(principal.storeId(), productId);
		return ResponseEntity.noContent().build();
	}
}
