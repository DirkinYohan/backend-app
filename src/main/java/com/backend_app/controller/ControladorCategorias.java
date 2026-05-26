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
import com.backend_app.dto.RespuestaCategoria;
import com.backend_app.dto.SolicitudCategoriaUpsert;
import com.backend_app.service.ServicioCategorias;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/categories")
public class ControladorCategorias {
	private final ServicioCategorias servicioCategorias;

	public ControladorCategorias(ServicioCategorias servicioCategorias) {
		this.servicioCategorias = servicioCategorias;
	}

	@GetMapping
	public ResponseEntity<List<RespuestaCategoria>> list(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@RequestParam(name = "q", required = false) String q) {
		return ResponseEntity.ok(servicioCategorias.list(principal.storeId(), q));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping
	public ResponseEntity<RespuestaCategoria> create(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@Valid @RequestBody SolicitudCategoriaUpsert request) {
		return ResponseEntity.ok(servicioCategorias.create(principal.storeId(), request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PutMapping("/{categoryId}")
	public ResponseEntity<RespuestaCategoria> update(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal,
			@PathVariable UUID categoryId, @Valid @RequestBody SolicitudCategoriaUpsert request) {
		return ResponseEntity.ok(servicioCategorias.update(principal.storeId(), categoryId, request));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@DeleteMapping("/{categoryId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID categoryId) {
		servicioCategorias.delete(principal.storeId(), categoryId);
		return ResponseEntity.noContent().build();
	}
}
