package com.backend_app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend_app.model.ImagenProducto;

public interface RepositorioImagenesProducto extends JpaRepository<ImagenProducto, UUID> {
	@Query("""
			select i.id from ProductImage i
			where i.store.id = :storeId and i.product.id = :productId
			order by i.sortOrder asc, i.createdAt asc
			""")
	List<UUID> listIds(@Param("storeId") UUID storeId, @Param("productId") UUID productId);

	Optional<ImagenProducto> findByIdAndStore_Id(UUID id, UUID storeId);

	@Query("""
			select i.id from ProductImage i
			where i.store.id = :storeId and i.product.id = :productId
			order by i.sortOrder asc, i.createdAt asc
			""")
	List<UUID> listIds(@Param("storeId") UUID storeId, @Param("productId") UUID productId, Pageable pageable);
}
