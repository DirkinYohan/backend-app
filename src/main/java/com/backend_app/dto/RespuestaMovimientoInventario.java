package com.backend_app.dto;

import java.time.Instant;
import java.util.UUID;

import com.backend_app.model.TipoMovimiento;

public record RespuestaMovimientoInventario(
		UUID id,
		TipoMovimiento movementType,
		UUID productId,
		String productName,
		String productCode,
		int quantity,
		RespuestaUsuario user,
		String observation,
		Instant createdAt) {
}
