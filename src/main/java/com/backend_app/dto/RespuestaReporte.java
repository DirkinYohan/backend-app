package com.backend_app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record RespuestaReporte(
		Instant from,
		Instant to,
		BigDecimal totalSales,
		BigDecimal totalProfit,
		long totalSalesCount,
		long totalProductsSold,
		List<PuntoDiario> dailySales,
		List<PuntoMensual> monthlySales,
		List<ProductoTop> topProducts,
		List<ProductoStockBajo> lowStockProducts) {
	public record PuntoDiario(LocalDate date, BigDecimal total, UUID productPrimaryImageId, String operatorName) {
	}

	public record PuntoMensual(YearMonth month, BigDecimal total) {
	}

	public record ProductoTop(UUID productId, UUID productPrimaryImageId, String code, String name, long unitsSold) {
	}

	public record ProductoStockBajo(UUID productId, UUID productPrimaryImageId, String code, String name, int stockCurrent,
			int stockMinimum) {
	}
}
