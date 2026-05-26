package com.backend_app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaProducto(
		UUID id,
		UUID categoryId,
		String categoryName,
		String name,
		String code,
		String description,
		List<UUID> imageIds,
		BigDecimal purchasePrice,
		BigDecimal salePrice,
		int stockCurrent,
		int stockMinimum,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
