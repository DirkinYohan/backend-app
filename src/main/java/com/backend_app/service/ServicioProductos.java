package com.backend_app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaProducto;
import com.backend_app.dto.SolicitudActualizarStockProducto;
import com.backend_app.dto.SolicitudProductoUpsert;
import com.backend_app.model.MovimientoInventario;
import com.backend_app.model.TipoMovimiento;
import com.backend_app.model.Categoria;
import com.backend_app.model.Producto;
import com.backend_app.model.Tienda;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioCategorias;
import com.backend_app.repository.RepositorioImagenesProducto;
import com.backend_app.repository.RepositorioMovimientosInventario;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.repository.RepositorioTiendas;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.ExcepcionSolicitudIncorrecta;

/**
 * Servicio encargado de la gestión de productos e inventario.
 * Permite realizar operaciones CRUD, búsqueda, ajuste de stock y alertas de bajo inventario.
 */
@Service
public class ServicioProductos {
	private final RepositorioProductos productRepository;
	private final RepositorioCategorias categoryRepository;
	private final RepositorioTiendas storeRepository;
	private final RepositorioUsuarios userRepository;
	private final RepositorioMovimientosInventario movementRepository;
	private final RepositorioImagenesProducto productImageRepository;

	public ServicioProductos(RepositorioProductos productRepository, RepositorioCategorias categoryRepository,
			RepositorioTiendas storeRepository, RepositorioUsuarios userRepository, RepositorioMovimientosInventario movementRepository,
			RepositorioImagenesProducto productImageRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.storeRepository = storeRepository;
		this.userRepository = userRepository;
		this.movementRepository = movementRepository;
		this.productImageRepository = productImageRepository;
	}

	/**
	 * Crea un nuevo producto en la tienda.
	 * 
	 * @param storeId ID de la tienda.
	 * @param request Datos del producto a crear.
	 * @return El producto creado en formato DTO.
	 */
	@Transactional
	public RespuestaProducto create(UUID storeId, SolicitudProductoUpsert request) {
		String code = normalizeCode(request.code());
		// Validar que el código de producto sea único por tienda
		if (productRepository.existsByStore_IdAndCodeIgnoreCase(storeId, code)) {
			throw new ExcepcionConflicto("El código ya existe");
		}
		if (request.initialStock() < 0) {
			throw new ExcepcionSolicitudIncorrecta("Stock inicial inválido");
		}

		Tienda store = storeRepository.findById(storeId).orElseThrow(() -> new ExcepcionNoEncontrado("Tienda no encontrada"));
		Categoria category = categoryRepository.findByIdAndStore_Id(request.categoryId(), storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Categoría no encontrada"));

		Producto product = new Producto();
		product.setStore(store);
		product.setCategory(category);
		product.setName(normalizeName(request.name()));
		product.setCode(code);
		product.setDescription(normalizeOptional(request.description()));
		product.setPurchasePrice(request.purchasePrice());
		product.setSalePrice(request.salePrice());
		product.setStockCurrent(request.initialStock());
		product.setStockMinimum(Math.max(0, request.stockMinimum()));
		product = productRepository.save(product);

		return toResponse(storeId, product);
	}

	/**
	 * Busca productos por nombre, código, estado y categoría.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaProducto> search(UUID storeId, String q, Boolean active, UUID categoryId) {
		String query = (q == null || q.isBlank()) ? null : q.trim();
		return productRepository.search(storeId, query, active, categoryId).stream().map(p -> toResponse(storeId, p)).toList();
	}

	/**
	 * Obtiene un producto específico por su ID.
	 */
	@Transactional(readOnly = true)
	public RespuestaProducto get(UUID storeId, UUID productId) {
		Producto product = productRepository.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
		return toResponse(storeId, product);
	}

	/**
	 * Actualiza la información de un producto existente.
	 */
	@Transactional
	public RespuestaProducto update(UUID storeId, UUID productId, SolicitudProductoUpsert request) {
		Producto product = productRepository.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));

		String code = normalizeCode(request.code());
		// Validar unicidad del código si ha cambiado
		if (!code.equalsIgnoreCase(product.getCode()) && productRepository.existsByStore_IdAndCodeIgnoreCase(storeId, code)) {
			throw new ExcepcionConflicto("El código ya existe");
		}
		Categoria category = categoryRepository.findByIdAndStore_Id(request.categoryId(), storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Categoría no encontrada"));

		product.setCategory(category);
		product.setName(normalizeName(request.name()));
		product.setCode(code);
		product.setDescription(normalizeOptional(request.description()));
		product.setPurchasePrice(request.purchasePrice());
		product.setSalePrice(request.salePrice());
		product.setStockMinimum(Math.max(0, request.stockMinimum()));
		product = productRepository.save(product);

		return toResponse(storeId, product);
	}

	/**
	 * Elimina un producto. Si tiene registros asociados (ventas o movimientos), 
	 * solo se desactiva lógicamente.
	 */
	@Transactional
	public void delete(UUID storeId, UUID productId) {
		Producto product = productRepository.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
		long saleRefs = productRepository.countSaleItemsByProduct(storeId, productId);
		long movementRefs = movementRepository.countByStoreIdAndProductId(storeId, productId);
		
		if (saleRefs > 0 || movementRefs > 0) {
			product.setActive(false);
			productRepository.save(product);
			return;
		}
		productRepository.delete(product);
	}

	/**
	 * Activa o desactiva un producto.
	 */
	@Transactional
	public RespuestaProducto setActive(UUID storeId, UUID productId, boolean active) {
		Producto product = productRepository.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
		product.setActive(active);
		product = productRepository.save(product);
		return toResponse(storeId, product);
	}

	/**
	 * Realiza un ajuste manual de stock y registra el movimiento.
	 * 
	 * @param storeId ID de la tienda.
	 * @param userId ID del usuario que realiza el ajuste.
	 * @param productId ID del producto.
	 * @param request Contiene el nuevo stock y observaciones.
	 * @return El producto con el stock actualizado.
	 */
	@Transactional
	public RespuestaProducto updateStock(UUID storeId, UUID userId, UUID productId, SolicitudActualizarStockProducto request) {
		Producto product = productRepository.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
		Usuario user = userRepository.findByIdAndStore_Id(userId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));

		int oldStock = product.getStockCurrent();
		int newStock = request.newStock();
		if (newStock < 0) {
			throw new ExcepcionSolicitudIncorrecta("Stock inválido");
		}
		if (oldStock == newStock) {
			return toResponse(storeId, product);
		}

		product.setStockCurrent(newStock);
		product = productRepository.save(product);

		// Registrar el movimiento de ajuste en el historial
		MovimientoInventario movement = new MovimientoInventario();
		movement.setStore(product.getStore());
		movement.setMovementType(TipoMovimiento.ADJUSTMENT);
		movement.setProduct(product);
		movement.setQuantity(newStock - oldStock);
		movement.setUser(user);
		movement.setObservation(normalizeOptional(request.observation()));
		movementRepository.save(movement);

		return toResponse(storeId, product);
	}

	/**
	 * Obtiene una lista de productos con stock por debajo del mínimo establecido.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaProducto> lowStock(UUID storeId, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, 50));
		return productRepository.lowStockProducts(storeId, PageRequest.of(0, safeLimit)).stream().map(p -> toResponse(storeId, p))
				.toList();
	}

	/**
	 * Convierte una entidad Producto a su representación DTO de respuesta.
	 */
	private RespuestaProducto toResponse(UUID storeId, Producto product) {
		return new RespuestaProducto(product.getId(), product.getCategory().getId(), product.getCategory().getName(), product.getName(),
				product.getCode(), product.getDescription(), productImageRepository.listIds(storeId, product.getId()),
				product.getPurchasePrice(), product.getSalePrice(), product.getStockCurrent(), product.getStockMinimum(),
				product.isActive(), product.getCreatedAt(), product.getUpdatedAt());
	}

	private static String normalizeName(String value) {
		return value.trim();
	}

	private static String normalizeCode(String value) {
		return value.trim();
	}

	private static String normalizeOptional(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}
}
