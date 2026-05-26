package com.backend_app.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudProductoUpsert(
		@NotBlank @Size(max = 140) String name,
		@NotBlank @Size(max = 60) String code,
		@Size(max = 1000) String description,
		@NotNull @DecimalMin("0.00") BigDecimal purchasePrice,
		@NotNull @DecimalMin("0.00") BigDecimal salePrice,
		@Min(0) int initialStock,
		@Min(0) int stockMinimum,
		@NotNull UUID categoryId) {
}
