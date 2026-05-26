package com.backend_app.dto;

import java.util.UUID;

import com.backend_app.model.TipoMovimiento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudCrearMovimientoInventario(
		@NotNull TipoMovimiento movementType,
		@NotNull UUID productId,
		@Min(1) int quantity,
		@Size(max = 500) String observation) {
}
