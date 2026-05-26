package com.backend_app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SolicitudActualizarStockProducto(
		@Min(0) int newStock,
		@Size(max = 500) String observation) {
}
