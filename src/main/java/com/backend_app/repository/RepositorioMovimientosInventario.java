package com.backend_app.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend_app.model.MovimientoInventario;

public interface RepositorioMovimientosInventario extends JpaRepository<MovimientoInventario, UUID> {
	@Query("""
			select m from InventoryMovement m
			where m.store.id = :storeId
			  and (:from is null or m.createdAt >= :from)
			  and (:to is null or m.createdAt <= :to)
			order by m.createdAt desc
			""")
	List<MovimientoInventario> list(@Param("storeId") UUID storeId, @Param("from") Instant from, @Param("to") Instant to,
			Pageable pageable);

	@Query("""
			select count(m) from InventoryMovement m
			where m.store.id = :storeId and m.product.id = :productId
			""")
	long countByStoreIdAndProductId(@Param("storeId") UUID storeId, @Param("productId") UUID productId);
}
