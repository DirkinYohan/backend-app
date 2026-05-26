package com.backend_app.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaVenta;
import com.backend_app.dto.SolicitudCrearVenta;
import com.backend_app.model.MovimientoInventario;
import com.backend_app.model.TipoMovimiento;
import com.backend_app.model.ItemVenta;
import com.backend_app.model.Producto;
import com.backend_app.model.Tienda;
import com.backend_app.model.Usuario;
import com.backend_app.model.Venta;
import com.backend_app.repository.RepositorioImagenesProducto;
import com.backend_app.repository.RepositorioMovimientosInventario;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.repository.RepositorioTiendas;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.repository.RepositorioVentas;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.ExcepcionSolicitudIncorrecta;
import com.backend_app.util.MapeadorUsuario;

@Service
public class ServicioVentas {
	private final RepositorioVentas saleRepository;
	private final RepositorioProductos productRepository;
	private final RepositorioImagenesProducto productImageRepository;
	private final RepositorioTiendas storeRepository;
	private final RepositorioUsuarios userRepository;
	private final RepositorioMovimientosInventario movementRepository;

	public ServicioVentas(RepositorioVentas saleRepository, RepositorioProductos productRepository, RepositorioTiendas storeRepository,
			RepositorioUsuarios userRepository, RepositorioMovimientosInventario movementRepository, RepositorioImagenesProducto productImageRepository) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.storeRepository = storeRepository;
		this.userRepository = userRepository;
		this.movementRepository = movementRepository;
		this.productImageRepository = productImageRepository;
	}

	@Transactional
	public RespuestaVenta create(UUID storeId, UUID operatorUserId, SolicitudCrearVenta request) {
		Tienda store = storeRepository.findById(storeId).orElseThrow(() -> new ExcepcionNoEncontrado("Tienda no encontrada"));
		Usuario operator = userRepository.findByIdAndStore_Id(operatorUserId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
		if (!operator.isActive()) {
			throw new ExcepcionSolicitudIncorrecta("Usuario inactivo");
		}

		List<SolicitudCrearVenta.Item> items = request.items().stream()
				.sorted(Comparator.comparing(SolicitudCrearVenta.Item::productId)).toList();
		if (items.isEmpty()) {
			throw new ExcepcionSolicitudIncorrecta("La venta debe tener productos");
		}

		Venta sale = new Venta();
		sale.setStore(store);
		sale.setOperator(operator);
		sale.setSaleNumber(generateSaleNumber());
		sale.setPaymentMethod(request.paymentMethod());

		BigDecimal total = BigDecimal.ZERO;
		BigDecimal totalProfit = BigDecimal.ZERO;

		List<ItemVenta> saleItems = new ArrayList<>();
		List<MovimientoInventario> movements = new ArrayList<>();

		for (SolicitudCrearVenta.Item itemReq : items) {
			if (itemReq.quantity() <= 0) {
				throw new ExcepcionSolicitudIncorrecta("Cantidad inválida");
			}
			Producto product = productRepository.findByIdAndStore_Id(itemReq.productId(), storeId)
					.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
			if (!product.isActive()) {
				throw new ExcepcionSolicitudIncorrecta("Producto inactivo: " + product.getCode());
			}
			if (product.getStockCurrent() < itemReq.quantity()) {
				throw new ExcepcionSolicitudIncorrecta("Stock insuficiente para: " + product.getCode());
			}

			int newStock = product.getStockCurrent() - itemReq.quantity();
			product.setStockCurrent(newStock);
			productRepository.save(product);

			BigDecimal unitPrice = product.getSalePrice();
			BigDecimal purchasePrice = product.getPurchasePrice();
			BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
			BigDecimal profit = unitPrice.subtract(purchasePrice).multiply(BigDecimal.valueOf(itemReq.quantity()));

			ItemVenta saleItem = new ItemVenta();
			saleItem.setSale(sale);
			saleItem.setProduct(product);
			saleItem.setQuantity(itemReq.quantity());
			saleItem.setPurchasePriceAtSale(purchasePrice);
			saleItem.setSalePriceAtSale(unitPrice);
			saleItem.setSubtotal(subtotal);
			saleItem.setProfit(profit);
			saleItems.add(saleItem);

			total = total.add(subtotal);
			totalProfit = totalProfit.add(profit);

			MovimientoInventario movement = new MovimientoInventario();
			movement.setStore(store);
			movement.setMovementType(TipoMovimiento.SALE);
			movement.setProduct(product);
			movement.setQuantity(-itemReq.quantity());
			movement.setUser(operator);
			movement.setObservation("Venta " + sale.getSaleNumber());
			movements.add(movement);
		}

		sale.setTotal(total);
		sale.setTotalProfit(totalProfit);
		sale.setItems(saleItems);
		sale = saleRepository.save(sale);

		movementRepository.saveAll(movements);

		return toResponseWithImages(sale);
	}

	@Transactional(readOnly = true)
	public List<RespuestaVenta> listForAdmin(UUID storeId, String q, Instant from, Instant to, UUID operatorId, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 200));
		Instant safeFrom = normalizeFromTo(from, to).from();
		Instant safeTo = normalizeFromTo(from, to).to();
		String query = (q == null || q.isBlank()) ? null : q.trim();

		return saleRepository.search(storeId, query, safeFrom, safeTo, operatorId, PageRequest.of(0, safeLimit)).stream()
				.map(this::toResponseWithImages).toList();
	}

	@Transactional(readOnly = true)
	public List<RespuestaVenta> listForOperator(UUID storeId, UUID operatorId, int limit) {
		return listForOperator(storeId, operatorId, null, null, null, limit);
	}

	@Transactional(readOnly = true)
	public List<RespuestaVenta> listForOperator(UUID storeId, UUID operatorId, String q, Instant from, Instant to, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 200));
		Instant safeFrom = normalizeFromTo(from, to).from();
		Instant safeTo = normalizeFromTo(from, to).to();
		String query = (q == null || q.isBlank()) ? null : q.trim();
		return saleRepository.search(storeId, query, safeFrom, safeTo, operatorId, PageRequest.of(0, safeLimit)).stream()
				.map(this::toResponseWithImages).toList();
	}

	@Transactional(readOnly = true)
	public RespuestaVenta get(UUID storeId, UUID saleId) {
		Venta sale = saleRepository.findByIdAndStore_Id(saleId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Venta no encontrada"));
		return toResponseWithImages(sale);
	}

	@Transactional(readOnly = true)
	public List<Venta> recent(UUID storeId, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 20));
		Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
		return saleRepository.search(storeId, null, from, null, null, PageRequest.of(0, safeLimit));
	}

	private RespuestaVenta toResponseWithImages(Venta sale) {
		UUID storeId = sale.getStore().getId();
		List<RespuestaVenta.Item> items = sale.getItems().stream()
				.map(i -> new RespuestaVenta.Item(i.getProduct().getId(), primaryImageId(storeId, i.getProduct().getId()),
						i.getProduct().getName(), i.getProduct().getCode(), i.getQuantity(), i.getSalePriceAtSale(),
						i.getSubtotal()))
				.toList();

		return new RespuestaVenta(sale.getId(), sale.getSaleNumber(), sale.getPaymentMethod(), sale.getTotal(), sale.getTotalProfit(),
				sale.getCreatedAt(), MapeadorUsuario.aRespuestaUsuario(sale.getOperator()), items);
	}

	private UUID primaryImageId(UUID storeId, UUID productId) {
		List<UUID> ids = productImageRepository.listIds(storeId, productId, PageRequest.of(0, 1));
		return ids.isEmpty() ? null : ids.get(0);
	}

	private static Range normalizeFromTo(Instant from, Instant to) {
		if (from != null && to != null && from.isAfter(to)) {
			return new Range(to, to);
		}
		return new Range(from, to);
	}

	private record Range(Instant from, Instant to) {
	}

	private static String generateSaleNumber() {
		String token = UUID.randomUUID().toString().replace("-", "");
		return "V-" + token.substring(0, 10).toUpperCase();
	}
}
