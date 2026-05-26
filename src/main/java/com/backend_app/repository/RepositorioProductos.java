package com.backend_app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend_app.model.Producto;

public interface RepositorioProductos extends JpaRepository<Producto, UUID> {
	Optional<Producto> findByIdAndStore_Id(UUID id, UUID storeId);

	boolean existsByStore_IdAndCodeIgnoreCase(UUID storeId, String code);

	long countByStore_Id(UUID storeId);

	@Query("""
			select count(p) from Product p
			where p.store.id = :storeId and p.active = true and p.stockCurrent <= p.stockMinimum
			""")
	long lowStockCount(@Param("storeId") UUID storeId);

	@Query("""
			select p from Product p
			where p.store.id = :storeId
			  and (:active is null or p.active = :active)
			  and (:categoryId is null or p.category.id = :categoryId)
			  and (
			        lower(p.name) like concat('%', lower(coalesce(:q, p.name)), '%')
			        or lower(p.code) like concat('%', lower(coalesce(:q, p.code)), '%')
			      )
			order by p.createdAt desc
			""")
	List<Producto> search(@Param("storeId") UUID storeId, @Param("q") String q, @Param("active") Boolean active,
			@Param("categoryId") UUID categoryId);

	@Query("""
			select p from Product p
			where p.store.id = :storeId and p.active = true and p.stockCurrent <= p.stockMinimum
			order by (p.stockMinimum - p.stockCurrent) desc, p.updatedAt desc
			""")
	List<Producto> lowStockProducts(@Param("storeId") UUID storeId, Pageable pageable);

	@Query("""
			select si.product.id, si.product.name, si.product.code, sum(si.quantity)
			from SaleItem si
			join si.sale s
			where s.store.id = :storeId
			group by si.product.id, si.product.name, si.product.code
			order by sum(si.quantity) desc
			""")
	List<Object[]> topProducts(@Param("storeId") UUID storeId, Pageable pageable);

	@Query("""
			select count(si) from SaleItem si
			join si.sale s
			where s.store.id = :storeId and si.product.id = :productId
			""")
	long countSaleItemsByProduct(@Param("storeId") UUID storeId, @Param("productId") UUID productId);
}
