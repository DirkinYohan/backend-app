package com.backend_app.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "SaleItem")
@Table(name = "sale_items")
public class ItemVenta {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id", nullable = false)
	private Venta sale;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Producto product;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "purchase_price_at_sale", nullable = false, precision = 12, scale = 2)
	//cuánto costaba el producto para el negocio cuando se vendió.
	private BigDecimal purchasePriceAtSale;

	@Column(name = "sale_price_at_sale", nullable = false, precision = 12, scale = 2)
	//a cuánto se vendió.
	private BigDecimal salePriceAtSale;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "profit", nullable = false, precision = 12, scale = 2)
	//cuánto ganó el negocio con la venta del producto.
	private BigDecimal profit;
}
