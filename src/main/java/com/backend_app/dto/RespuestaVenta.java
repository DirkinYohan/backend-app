package com.backend_app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.backend_app.model.MetodoPago;

public record RespuestaVenta(
		UUID id,
		String saleNumber,
		MetodoPago paymentMethod,
		BigDecimal total,
		BigDecimal totalProfit,
		Instant createdAt,
		RespuestaUsuario operator,
		List<Item> items) {
	public record Item(UUID productId, UUID productPrimaryImageId, String productName, String productCode, int quantity, BigDecimal unitPrice,
			BigDecimal subtotal) {
	}
}
