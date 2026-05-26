package com.backend_app.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backend_app.model.ImagenProducto;
import com.backend_app.model.Producto;
import com.backend_app.repository.RepositorioImagenesProducto;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.ExcepcionSolicitudIncorrecta;

@Service
public class ServicioImagenesProducto {
	private static final long MAX_FILE_SIZE = 2L * 1024L * 1024L;
	private static final int MAX_IMAGES_PER_PRODUCT = 6;
	private static final List<String> ALLOWED_TYPES = List.of("image/png", "image/jpeg", "image/webp");

	private final RepositorioProductos repositorioProductos;
	private final RepositorioImagenesProducto repositorioImagenes;

	public ServicioImagenesProducto(RepositorioProductos repositorioProductos, RepositorioImagenesProducto repositorioImagenes) {
		this.repositorioProductos = repositorioProductos;
		this.repositorioImagenes = repositorioImagenes;
	}

	@Transactional
	public List<UUID> subir(UUID storeId, UUID productId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new ExcepcionSolicitudIncorrecta("Adjunta al menos una imagen");
		}

		Producto producto = repositorioProductos.findByIdAndStore_Id(productId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));

		List<UUID> existentes = repositorioImagenes.listIds(storeId, productId);
		if (existentes.size() >= MAX_IMAGES_PER_PRODUCT) {
			throw new ExcepcionSolicitudIncorrecta("Máximo " + MAX_IMAGES_PER_PRODUCT + " imágenes por producto");
		}
		if (existentes.size() + files.size() > MAX_IMAGES_PER_PRODUCT) {
			throw new ExcepcionSolicitudIncorrecta("Se supera el máximo de " + MAX_IMAGES_PER_PRODUCT + " imágenes");
		}

		int siguienteOrden = existentes.size();
		for (MultipartFile file : files) {
			validarArchivo(file);
			ImagenProducto img = new ImagenProducto();
			img.setStore(producto.getStore());
			img.setProduct(producto);
			img.setContentType(file.getContentType());
			img.setFileName(normalizarNombreArchivo(file.getOriginalFilename()));
			img.setSortOrder(siguienteOrden++);
			img.setData(leerBytes(file));
			repositorioImagenes.save(img);
		}

		return repositorioImagenes.listIds(storeId, productId);
	}

	@Transactional
	public void eliminar(UUID storeId, UUID productId, UUID imageId) {
		ImagenProducto imagen = repositorioImagenes.findByIdAndStore_Id(imageId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Imagen no encontrada"));
		if (!imagen.getProduct().getId().equals(productId)) {
			throw new ExcepcionNoEncontrado("Imagen no encontrada");
		}
		repositorioImagenes.delete(imagen);
	}

	@Transactional(readOnly = true)
	public List<UUID> listarIds(UUID storeId, UUID productId) {
		return repositorioImagenes.listIds(storeId, productId);
	}

	@Transactional(readOnly = true)
	public ImagenProducto obtenerImagen(UUID storeId, UUID imageId) {
		return repositorioImagenes.findByIdAndStore_Id(imageId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Imagen no encontrada"));
	}

	private static void validarArchivo(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ExcepcionSolicitudIncorrecta("Archivo inválido");
		}
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new ExcepcionSolicitudIncorrecta("Imagen demasiado grande (máx 2MB)");
		}
		String type = file.getContentType();
		if (type == null || !ALLOWED_TYPES.contains(type)) {
			throw new ExcepcionSolicitudIncorrecta("Formato no permitido (PNG/JPEG/WEBP)");
		}
	}

	private static byte[] leerBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new ExcepcionSolicitudIncorrecta("No se pudo leer la imagen");
		}
	}

	private static String normalizarNombreArchivo(String name) {
		if (name == null) return "image";
		String trimmed = name.trim();
		if (trimmed.isBlank()) return "image";
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}
}
