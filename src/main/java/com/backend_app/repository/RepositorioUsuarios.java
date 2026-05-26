package com.backend_app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend_app.model.Rol;
import com.backend_app.model.Usuario;

public interface RepositorioUsuarios extends JpaRepository<Usuario, UUID> {
	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	Optional<Usuario> findByIdAndStore_Id(UUID id, UUID storeId);

	List<Usuario> findAllByStore_IdAndRoleOrderByCreatedAtDesc(UUID storeId, Rol role);

	long countByStore_IdAndRole(UUID storeId, Rol role);
}
