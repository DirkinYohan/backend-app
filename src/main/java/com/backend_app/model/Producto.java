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

@Getter
@Setter
@Entity(name = "Product")
@Table(name = "products", uniqueConstraints = { @UniqueConstraint(name = "uq_products_store_code", columnNames = {
		"store_id", "code" }) })
public class Producto {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Tienda store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Categoria category;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(length = 1000)
	private String description;

	@Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal purchasePrice;

	@Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "stock_current", nullable = false)
	private int stockCurrent;

	@Column(name = "stock_minimum", nullable = false)
	private int stockMinimum;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

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

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}
