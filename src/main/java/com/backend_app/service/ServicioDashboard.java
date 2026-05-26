package com.backend_app.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaDashboard;
import com.backend_app.model.Rol;
import com.backend_app.model.Producto;
import com.backend_app.model.Venta;
import com.backend_app.repository.RepositorioCategorias;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.repository.RepositorioVentas;

@Service
public class ServicioDashboard {
	private final RepositorioProductos productRepository;
	private final RepositorioCategorias categoryRepository;
	private final RepositorioUsuarios userRepository;
	private final RepositorioVentas saleRepository;

	public ServicioDashboard(RepositorioProductos productRepository, RepositorioCategorias categoryRepository,
			RepositorioUsuarios userRepository, RepositorioVentas saleRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.userRepository = userRepository;
		this.saleRepository = saleRepository;
	}

	@Transactional(readOnly = true)
	public RespuestaDashboard adminDashboard(UUID storeId) {
		Instant last30 = Instant.now().minus(30, ChronoUnit.DAYS);
		List<Object[]> rows = saleRepository.metricasDashboardAdmin(storeId, last30);
		Object[] metricas = rows.isEmpty() ? new Object[] { 0, 0, 0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO } : rows.get(0);
		int totalProducts = ((Number) metricas[0]).intValue();
		int lowStockProducts = ((Number) metricas[1]).intValue();
		int totalCategories = ((Number) metricas[2]).intValue();
		int totalOperators = ((Number) metricas[3]).intValue();
		BigDecimal totalSales = (BigDecimal) metricas[4];
		long totalSalesCount = ((Number) metricas[5]).longValue();
		BigDecimal totalSalesLast30Days = (BigDecimal) metricas[6];
		long totalSalesCountLast30Days = ((Number) metricas[7]).longValue();
		BigDecimal totalProfit = (BigDecimal) metricas[8];

		List<RespuestaDashboard.ResumenVenta> recentSales = recentSales(storeId, null);
		List<RespuestaDashboard.ProductoTop> topProducts = topProducts(storeId);
		List<RespuestaDashboard.PuntoVentasDiarias> dailySales = dailySales(storeId, null);

		return new RespuestaDashboard(totalProducts, lowStockProducts, totalCategories, totalOperators, totalSales, totalSalesCount,
				totalSalesLast30Days, totalSalesCountLast30Days, totalProfit, recentSales, topProducts, dailySales);
	}

	@Transactional(readOnly = true)
	public RespuestaDashboard operatorDashboard(UUID storeId, UUID operatorId) {
		int totalProducts = (int) productRepository.countByStore_Id(storeId);
		int lowStockProducts = (int) productRepository.lowStockCount(storeId);
		int totalCategories = (int) categoryRepository.countByStore_Id(storeId);

		BigDecimal totalSales = saleRepository.totalSalesByOperator(storeId, operatorId);
		long totalSalesCount = saleRepository.countByStore_IdAndOperator_Id(storeId, operatorId);
		Instant last30 = Instant.now().minus(30, ChronoUnit.DAYS);
		BigDecimal totalSalesLast30Days = saleRepository.totalSalesSince(storeId, operatorId, last30);
		long totalSalesCountLast30Days = saleRepository.countSince(storeId, operatorId, last30);

		List<RespuestaDashboard.ResumenVenta> recentSales = recentSales(storeId, operatorId);
		List<RespuestaDashboard.PuntoVentasDiarias> dailySales = dailySales(storeId, operatorId);

		return new RespuestaDashboard(totalProducts, lowStockProducts, totalCategories, null, totalSales, totalSalesCount,
				totalSalesLast30Days, totalSalesCountLast30Days, null, recentSales, List.of(), dailySales);
	}

	private List<RespuestaDashboard.ResumenVenta> recentSales(UUID storeId, UUID operatorId) {
		Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
		List<Venta> sales = saleRepository.search(storeId, null, from, null, operatorId, PageRequest.of(0, 10));

		return sales.stream().map(s -> new RespuestaDashboard.ResumenVenta(s.getId(), s.getSaleNumber(), s.getTotal(), s.getCreatedAt(),
				s.getOperator().getFirstName() + " " + s.getOperator().getLastName())).toList();
	}

	private List<RespuestaDashboard.ProductoTop> topProducts(UUID storeId) {
		List<Object[]> raw = productRepository.topProducts(storeId, PageRequest.of(0, 5));
		List<RespuestaDashboard.ProductoTop> result = new ArrayList<>();
		for (Object[] row : raw) {
			UUID productId = (UUID) row[0];
			String name = (String) row[1];
			String code = (String) row[2];
			long unitsSold = ((Number) row[3]).longValue();
			result.add(new RespuestaDashboard.ProductoTop(productId, name, code, unitsSold));
		}
		return result;
	}

	private List<RespuestaDashboard.PuntoVentasDiarias> dailySales(UUID storeId, UUID operatorId) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zone);
		LocalDate fromDate = today.minusDays(6);
		Instant fromInstant = fromDate.atStartOfDay(zone).toInstant();

		List<Venta> sales = saleRepository.search(storeId, null, fromInstant, null, null, PageRequest.of(0, 10_000));
		Map<LocalDate, BigDecimal> totals = new HashMap<>();

		for (Venta sale : sales) {
			if (operatorId != null && !sale.getOperator().getId().equals(operatorId)) continue;
			LocalDate date = sale.getCreatedAt().atZone(zone).toLocalDate();
			if (date.isBefore(fromDate) || date.isAfter(today)) continue;
			totals.merge(date, sale.getTotal(), BigDecimal::add);
		}

		List<RespuestaDashboard.PuntoVentasDiarias> points = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			LocalDate date = fromDate.plusDays(i);
			points.add(new RespuestaDashboard.PuntoVentasDiarias(date, totals.getOrDefault(date, BigDecimal.ZERO)));
		}
		return points;
	}
}
