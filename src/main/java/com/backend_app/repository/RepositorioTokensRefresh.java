package com.backend_app.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend_app.model.TokenRefresh;

public interface RepositorioTokensRefresh extends JpaRepository<TokenRefresh, UUID> {
	Optional<TokenRefresh> findByTokenHash(String tokenHash);
}
