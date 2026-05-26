package com.backend_app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend_app.model.Categoria;

public interface RepositorioCategorias extends JpaRepository<Categoria, UUID> {
	List<Categoria> findAllByStore_IdOrderByNameAsc(UUID storeId);

	List<Categoria> findAllByStore_IdAndNameContainingIgnoreCaseOrderByNameAsc(UUID storeId, String name);

	Optional<Categoria> findByIdAndStore_Id(UUID id, UUID storeId);

	boolean existsByStore_IdAndNameIgnoreCase(UUID storeId, String name);

	long countByStore_Id(UUID storeId);
}
