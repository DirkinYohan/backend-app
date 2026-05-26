package com.backend_app.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backend_app.model.Venta;

public interface RepositorioVentas extends JpaRepository<Venta, UUID> {
	Optional<Venta> findByIdAndStore_Id(UUID id, UUID storeId);

	List<Venta> findAllByStore_IdOrderByCreatedAtDesc(UUID storeId);

	List<Venta> findAllByStore_IdAndOperator_IdOrderByCreatedAtDesc(UUID storeId, UUID operatorId);

	@Query("""
			select coalesce(sum(s.total), 0) from Sale s
			where s.store.id = :storeId
			""")
	BigDecimal totalSales(@Param("storeId") UUID storeId);

	@Query("""
			select coalesce(sum(s.total), 0) from Sale s
			where s.store.id = :storeId and s.operator.id = :operatorId
			""")
	BigDecimal totalSalesByOperator(@Param("storeId") UUID storeId, @Param("operatorId") UUID operatorId);

	@Query("""
			select coalesce(sum(s.totalProfit), 0) from Sale s
			where s.store.id = :storeId
			""")
	BigDecimal totalProfit(@Param("storeId") UUID storeId);

	long countByStore_Id(UUID storeId);

	long countByStore_IdAndOperator_Id(UUID storeId, UUID operatorId);

	@Query("""
			select count(s) from Sale s
			where s.store.id = :storeId
			  and (:operatorId is null or s.operator.id = :operatorId)
			  and s.createdAt >= :from
			""")
	long countSince(@Param("storeId") UUID storeId, @Param("operatorId") UUID operatorId, @Param("from") Instant from);

	@Query("""
			select coalesce(sum(s.total), 0) from Sale s
			where s.store.id = :storeId
			  and (:operatorId is null or s.operator.id = :operatorId)
			  and s.createdAt >= :from
			""")
	BigDecimal totalSalesSince(@Param("storeId") UUID storeId, @Param("operatorId") UUID operatorId, @Param("from") Instant from);

	@Query("""
			select s from Sale s
			where s.store.id = :storeId
			  and s.createdAt >= coalesce(:from, s.createdAt)
			  and s.createdAt <= coalesce(:to, s.createdAt)
			  and s.operator.id = coalesce(:operatorId, s.operator.id)
			  and lower(s.saleNumber) like concat('%', lower(coalesce(:q, s.saleNumber)), '%')
			order by s.createdAt desc
			""")
	List<Venta> search(@Param("storeId") UUID storeId, @Param("q") String q, @Param("from") Instant from, @Param("to") Instant to,
			@Param("operatorId") UUID operatorId, Pageable pageable);

	@Query(value = """
			select
			  (select count(*) from products p where p.store_id = :storeId) as total_products,
			  (select count(*) from products p where p.store_id = :storeId and p.active = true and p.stock_current <= p.stock_minimum) as low_stock_products,
			  (select count(*) from categories c where c.store_id = :storeId) as total_categories,
			  (select count(*) from users u where u.store_id = :storeId and u.role = 'OPERADOR') as total_operators,
			  (select coalesce(sum(s.total), 0) from sales s where s.store_id = :storeId) as total_sales,
			  (select count(*) from sales s where s.store_id = :storeId) as total_sales_count,
			  (select coalesce(sum(s.total), 0) from sales s where s.store_id = :storeId and s.created_at >= :last30) as total_sales_last_30_days,
			  (select count(*) from sales s where s.store_id = :storeId and s.created_at >= :last30) as total_sales_count_last_30_days,
			  (select coalesce(sum(s.total_profit), 0) from sales s where s.store_id = :storeId) as total_profit
			""", nativeQuery = true)
	List<Object[]> metricasDashboardAdmin(@Param("storeId") UUID storeId, @Param("last30") Instant last30);
}
