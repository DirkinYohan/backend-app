package com.backend_app.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad que representa un producto en el sistema.
 * Contiene información sobre precios, stock, categoría y tienda asociada.
 */
@Getter
@Setter
@Entity(name = "Product")
@Table(name = "products", uniqueConstraints = { @UniqueConstraint(name = "uq_products_store_code", columnNames = {
		"store_id", "code" }) })
public class Producto {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Relación con la tienda a la que pertenece el producto
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Tienda store;

	// Categoría del producto
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Categoria category;

	@Column(nullable = false, length = 140)
	private String name;

	// Código único para el producto dentro de una misma tienda
	@Column(nullable = false, length = 60)
	private String code;

	@Column(length = 1000)
	private String description;

	// Precio al que se compra el producto al proveedor
	@Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal purchasePrice;

	// Precio al que se vende el producto al cliente
	@Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal salePrice;

	// Cantidad actual disponible en inventario
	@Column(name = "stock_current", nullable = false)
	private int stockCurrent;

	// Cantidad mínima permitida antes de generar una alerta de bajo stock
	@Column(name = "stock_minimum", nullable = false)
	private int stockMinimum;

	// Estado del producto (habilitado/deshabilitado)
	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/**
	 * Método ejecutado automáticamente antes de persistir el objeto.
	 * Inicializa las fechas de creación y actualización.
	 */
	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	/**
	 * Método ejecutado automáticamente antes de actualizar el objeto.
	 * Actualiza la fecha de modificación.
	 */
	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
