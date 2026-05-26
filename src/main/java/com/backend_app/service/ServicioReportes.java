package com.backend_app.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaReporte;
import com.backend_app.model.Venta;
import com.backend_app.repository.RepositorioImagenesProducto;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.repository.RepositorioVentas;

@Service
public class ServicioReportes {
	private final RepositorioVentas repositorioVentas;
	private final RepositorioProductos repositorioProductos;
	private final RepositorioImagenesProducto repositorioImagenesProducto;

	public ServicioReportes(RepositorioVentas repositorioVentas, RepositorioProductos repositorioProductos,
			RepositorioImagenesProducto repositorioImagenesProducto) {
		this.repositorioVentas = repositorioVentas;
		this.repositorioProductos = repositorioProductos;
		this.repositorioImagenesProducto = repositorioImagenesProducto;
	}

	@Transactional(readOnly = true)
	public RespuestaReporte generarReporte(UUID storeId, Instant from, Instant to, UUID operatorId) {
		Rango rango = normalizarFromTo(from, to);

		List<Venta> ventas = repositorioVentas.search(storeId, null, rango.from(), rango.to(), operatorId, PageRequest.of(0, 10_000));
		BigDecimal totalVentas = BigDecimal.ZERO;
		BigDecimal totalGanancia = BigDecimal.ZERO;
		long totalProductosVendidos = 0;
		for (Venta v : ventas) {
			totalVentas = totalVentas.add(v.getTotal());
			totalGanancia = totalGanancia.add(v.getTotalProfit());
			if (v.getItems() != null) {
				for (var item : v.getItems()) {
					if (item == null) continue;
					totalProductosVendidos += item.getQuantity();
				}
			}
		}

		List<RespuestaReporte.PuntoDiario> diario = serieDiaria(ventas, rango);
		List<RespuestaReporte.PuntoMensual> mensual = serieMensual(ventas, rango);
		List<RespuestaReporte.ProductoTop> top = topProductos(storeId);
		List<RespuestaReporte.ProductoStockBajo> bajoStock = productosBajoStock(storeId);

		return new RespuestaReporte(rango.from(), rango.to(), totalVentas, totalGanancia, ventas.size(), totalProductosVendidos, diario, mensual,
				top, bajoStock);
	}

	private List<RespuestaReporte.PuntoDiario> serieDiaria(List<Venta> sales, Rango rango) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate fromDate = rango.from() == null ? null : rango.from().atZone(zone).toLocalDate();
		LocalDate toDate = rango.to() == null ? null : rango.to().atZone(zone).toLocalDate();
		if (fromDate == null || toDate == null) return List.of();

		Map<LocalDate, BigDecimal> totales = new HashMap<>();
		for (Venta sale : sales) {
			LocalDate day = sale.getCreatedAt().atZone(zone).toLocalDate();
			totales.merge(day, sale.getTotal(), BigDecimal::add);
		}

		List<LocalDate> dias = totales.entrySet().stream()
				.filter(e -> !e.getKey().isBefore(fromDate) && !e.getKey().isAfter(toDate))
				.sorted(Map.Entry.comparingByKey())
				.filter(e -> e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0)
				.map(Map.Entry::getKey)
				.toList();

		if (dias.isEmpty()) return List.of();
		List<LocalDate> ultimosDias = dias.size() <= 7 ? dias : dias.subList(dias.size() - 7, dias.size());

		Map<LocalDate, UUID> diaAImagen = new HashMap<>();
		Map<LocalDate, UUID> diaAOperadorId = new HashMap<>();
		Map<LocalDate, String> diaAOperadorNombre = new HashMap<>();
		Map<LocalDate, Boolean> diaAVariosOperadores = new HashMap<>();
		for (Venta sale : sales) {
			LocalDate day = sale.getCreatedAt().atZone(zone).toLocalDate();
			if (!ultimosDias.contains(day)) continue;

			UUID operadorId = sale.getOperator().getId();
			String operadorNombre = sale.getOperator().getFirstName() + " " + sale.getOperator().getLastName();
			UUID previoOperadorId = diaAOperadorId.get(day);
			if (previoOperadorId == null) {
				diaAOperadorId.put(day, operadorId);
				diaAOperadorNombre.put(day, operadorNombre);
			} else if (!previoOperadorId.equals(operadorId)) {
				diaAVariosOperadores.put(day, true);
			}

			if (diaAImagen.containsKey(day)) continue;
			if (sale.getItems() == null || sale.getItems().isEmpty()) continue;
			UUID productId = sale.getItems().get(0).getProduct().getId();
			diaAImagen.put(day, idImagenPrincipal(sale.getStore().getId(), productId));
			if (diaAImagen.size() == ultimosDias.size()) break;
		}

		List<RespuestaReporte.PuntoDiario> points = new ArrayList<>();
		for (LocalDate d : ultimosDias) {
			String operatorName = diaAVariosOperadores.getOrDefault(d, false) ? "Varios" : diaAOperadorNombre.get(d);
			points.add(new RespuestaReporte.PuntoDiario(d, totales.getOrDefault(d, BigDecimal.ZERO), diaAImagen.get(d), operatorName));
		}
		return points;
	}

	private List<RespuestaReporte.PuntoMensual> serieMensual(List<Venta> sales, Rango rango) {
		ZoneId zone = ZoneId.systemDefault();
		YearMonth fromMonth = rango.from() == null ? null : YearMonth.from(rango.from().atZone(zone));
		YearMonth toMonth = rango.to() == null ? null : YearMonth.from(rango.to().atZone(zone));
		if (fromMonth == null || toMonth == null) return List.of();

		Map<YearMonth, BigDecimal> totales = new HashMap<>();
		for (Venta sale : sales) {
			YearMonth ym = YearMonth.from(sale.getCreatedAt().atZone(zone));
			totales.merge(ym, sale.getTotal(), BigDecimal::add);
		}

		List<RespuestaReporte.PuntoMensual> points = new ArrayList<>();
		for (YearMonth m = fromMonth; !m.isAfter(toMonth); m = m.plusMonths(1)) {
			points.add(new RespuestaReporte.PuntoMensual(m, totales.getOrDefault(m, BigDecimal.ZERO)));
		}
		return points;
	}

	private List<RespuestaReporte.ProductoTop> topProductos(UUID storeId) {
		List<Object[]> raw = repositorioProductos.topProducts(storeId, PageRequest.of(0, 10));
		List<RespuestaReporte.ProductoTop> result = new ArrayList<>();
		for (Object[] row : raw) {
			UUID productId = (UUID) row[0];
			String name = (String) row[1];
			String code = (String) row[2];
			long unitsSold = ((Number) row[3]).longValue();
			result.add(new RespuestaReporte.ProductoTop(productId, idImagenPrincipal(storeId, productId), code, name, unitsSold));
		}
		return result;
	}

	private List<RespuestaReporte.ProductoStockBajo> productosBajoStock(UUID storeId) {
		return repositorioProductos.lowStockProducts(storeId, PageRequest.of(0, 20)).stream()
				.map(p -> new RespuestaReporte.ProductoStockBajo(p.getId(), idImagenPrincipal(storeId, p.getId()), p.getCode(), p.getName(),
						p.getStockCurrent(), p.getStockMinimum()))
				.toList();
	}

	private UUID idImagenPrincipal(UUID storeId, UUID productId) {
		List<UUID> ids = repositorioImagenesProducto.listIds(storeId, productId, PageRequest.of(0, 1));
		return ids.isEmpty() ? null : ids.get(0);
	}

	private static Rango normalizarFromTo(Instant from, Instant to) {
		if (from == null && to == null) {
			ZoneId zone = ZoneId.systemDefault();
			LocalDate today = LocalDate.now(zone);
			LocalDate start = today.minusDays(29);
			return new Rango(start.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
		}
		if (from != null && to != null && from.isAfter(to)) {
			return new Rango(to, to);
		}
		return new Rango(from, to);
	}

	private record Rango(Instant from, Instant to) {
	}
}
