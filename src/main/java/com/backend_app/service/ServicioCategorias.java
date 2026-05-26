package com.backend_app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaCategoria;
import com.backend_app.dto.SolicitudCategoriaUpsert;
import com.backend_app.model.Categoria;
import com.backend_app.model.Tienda;
import com.backend_app.repository.RepositorioCategorias;
import com.backend_app.repository.RepositorioTiendas;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoEncontrado;

@Service
public class ServicioCategorias {
	private final RepositorioCategorias categoryRepository;
	private final RepositorioTiendas storeRepository;

	public ServicioCategorias(RepositorioCategorias categoryRepository, RepositorioTiendas storeRepository) {
		this.categoryRepository = categoryRepository;
		this.storeRepository = storeRepository;
	}

	@Transactional
	public RespuestaCategoria create(UUID storeId, SolicitudCategoriaUpsert request) {
		String name = normalizeName(request.name());
		if (categoryRepository.existsByStore_IdAndNameIgnoreCase(storeId, name)) {
			throw new ExcepcionConflicto("La categoría ya existe");
		}

		Tienda store = storeRepository.findById(storeId).orElseThrow(() -> new ExcepcionNoEncontrado("Tienda no encontrada"));

		Categoria category = new Categoria();
		category.setStore(store);
		category.setName(name);
		category.setDescription(normalizeOptional(request.description()));
		category = categoryRepository.save(category);

		return toResponse(category);
	}

	@Transactional(readOnly = true)
	public List<RespuestaCategoria> list(UUID storeId, String q) {
		List<Categoria> categories = (q == null || q.isBlank())
				? categoryRepository.findAllByStore_IdOrderByNameAsc(storeId)
				: categoryRepository.findAllByStore_IdAndNameContainingIgnoreCaseOrderByNameAsc(storeId, q.trim());
		return categories.stream().map(ServicioCategorias::toResponse).toList();
	}

	@Transactional
	public RespuestaCategoria update(UUID storeId, UUID categoryId, SolicitudCategoriaUpsert request) {
		Categoria category = categoryRepository.findByIdAndStore_Id(categoryId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Categoría no encontrada"));

		String name = normalizeName(request.name());
		if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByStore_IdAndNameIgnoreCase(storeId, name)) {
			throw new ExcepcionConflicto("La categoría ya existe");
		}

		category.setName(name);
		category.setDescription(normalizeOptional(request.description()));
		category = categoryRepository.save(category);

		return toResponse(category);
	}

	@Transactional
	public void delete(UUID storeId, UUID categoryId) {
		Categoria category = categoryRepository.findByIdAndStore_Id(categoryId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Categoría no encontrada"));
		categoryRepository.delete(category);
	}

	private static RespuestaCategoria toResponse(Categoria category) {
		return new RespuestaCategoria(category.getId(), category.getName(), category.getDescription(), category.getCreatedAt());
	}

	private static String normalizeName(String value) {
		return value.trim();
	}

	private static String normalizeOptional(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}
}
