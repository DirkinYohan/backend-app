package com.backend_app.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend_app.model.Tienda;

public interface RepositorioTiendas extends JpaRepository<Tienda, UUID> {
	Optional<Tienda> findByAdminUserId(UUID adminUserId);
}
