package com.backend_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.dto.RespuestaDashboard;
import com.backend_app.model.Rol;
import com.backend_app.service.ServicioDashboard;

@Validated
@RestController
@RequestMapping("/api/dashboard")
public class ControladorDashboard {
	private final ServicioDashboard servicioDashboard;

	public ControladorDashboard(ServicioDashboard servicioDashboard) {
		this.servicioDashboard = servicioDashboard;
	}

	@GetMapping
	public ResponseEntity<RespuestaDashboard> dashboard(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal) {
		if (principal.role() == Rol.ADMINISTRADOR) {
			return ResponseEntity.ok(servicioDashboard.adminDashboard(principal.storeId()));
		}
		return ResponseEntity.ok(servicioDashboard.operatorDashboard(principal.storeId(), principal.userId()));
	}
}
