package com.backend_app.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend_app.config.PrincipalUsuarioAutenticado;
import com.backend_app.model.ImagenProducto;
import com.backend_app.service.ServicioImagenesProducto;

@Validated
@RestController
@RequestMapping("/api/products")
public class ControladorImagenesProducto {
	private final ServicioImagenesProducto servicioImagenes;

	public ControladorImagenesProducto(ServicioImagenesProducto servicioImagenes) {
		this.servicioImagenes = servicioImagenes;
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@PostMapping("/{productId}/images")
	public ResponseEntity<List<UUID>> subir(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@RequestParam("files") List<MultipartFile> files) {
		return ResponseEntity.ok(servicioImagenes.subir(principal.storeId(), productId, files));
	}

	@GetMapping("/{productId}/images")
	public ResponseEntity<List<UUID>> listar(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId) {
		return ResponseEntity.ok(servicioImagenes.listarIds(principal.storeId(), productId));
	}

	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@DeleteMapping("/{productId}/images/{imageId}")
	public ResponseEntity<Void> eliminar(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID productId,
			@PathVariable UUID imageId) {
		servicioImagenes.eliminar(principal.storeId(), productId, imageId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/images/{imageId}")
	public ResponseEntity<ByteArrayResource> obtener(@AuthenticationPrincipal PrincipalUsuarioAutenticado principal, @PathVariable UUID imageId) {
		ImagenProducto image = servicioImagenes.obtenerImagen(principal.storeId(), imageId);
		String contentType = image.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : image.getContentType();

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
				.body(new ByteArrayResource(image.getData()));
	}
}
