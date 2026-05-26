package com.backend_app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RespuestaDashboard(
		int totalProducts,
		int lowStockProducts,
		int totalCategories,
		Integer totalOperators,
		BigDecimal totalSales,
		Long totalSalesCount,
		BigDecimal totalSalesLast30Days,
		Long totalSalesCountLast30Days,
		BigDecimal totalProfit,
		List<ResumenVenta> recentSales,
		List<ProductoTop> topProducts,
		List<PuntoVentasDiarias> dailySales) {
	public record ResumenVenta(UUID id, String saleNumber, BigDecimal total, Instant createdAt, String operatorName) {
	}

	public record ProductoTop(UUID productId, String name, String code, long unitsSold) {
	}

	public record PuntoVentasDiarias(LocalDate date, BigDecimal total) {
	}
}
