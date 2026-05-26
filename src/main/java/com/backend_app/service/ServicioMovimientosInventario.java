package com.backend_app.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend_app.dto.RespuestaMovimientoInventario;
import com.backend_app.dto.SolicitudCrearMovimientoInventario;
import com.backend_app.model.MovimientoInventario;
import com.backend_app.model.TipoMovimiento;
import com.backend_app.model.Producto;
import com.backend_app.model.Usuario;
import com.backend_app.repository.RepositorioMovimientosInventario;
import com.backend_app.repository.RepositorioProductos;
import com.backend_app.repository.RepositorioUsuarios;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.ExcepcionSolicitudIncorrecta;
import com.backend_app.util.MapeadorUsuario;

@Service
public class ServicioMovimientosInventario {
	private final RepositorioMovimientosInventario repositorioMovimientos;
	private final RepositorioProductos repositorioProductos;
	private final RepositorioUsuarios repositorioUsuarios;

	public ServicioMovimientosInventario(RepositorioMovimientosInventario repositorioMovimientos,
			RepositorioProductos repositorioProductos, RepositorioUsuarios repositorioUsuarios) {
		this.repositorioMovimientos = repositorioMovimientos;
		this.repositorioProductos = repositorioProductos;
		this.repositorioUsuarios = repositorioUsuarios;
	}

	@Transactional
	public RespuestaMovimientoInventario crear(UUID storeId, UUID userId, SolicitudCrearMovimientoInventario request) {
		TipoMovimiento tipo = request.movementType();
		if (tipo == TipoMovimiento.SALE) {
			throw new ExcepcionSolicitudIncorrecta("Movimiento inválido");
		}

		Producto producto = repositorioProductos.findByIdAndStore_Id(request.productId(), storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Producto no encontrado"));
		Usuario usuario = repositorioUsuarios.findByIdAndStore_Id(userId, storeId)
				.orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));

		int delta = request.quantity();
		if (tipo == TipoMovimiento.EXIT) {
			delta = -delta;
		}

		int nuevoStock = producto.getStockCurrent() + delta;
		if (nuevoStock < 0) {
			throw new ExcepcionSolicitudIncorrecta("Stock insuficiente");
		}

		producto.setStockCurrent(nuevoStock);
		repositorioProductos.save(producto);

		MovimientoInventario movimiento = new MovimientoInventario();
		movimiento.setStore(producto.getStore());
		movimiento.setMovementType(tipo);
		movimiento.setProduct(producto);
		movimiento.setQuantity(delta);
		movimiento.setUser(usuario);
		movimiento.setObservation(normalizarOpcional(request.observation()));
		movimiento = repositorioMovimientos.save(movimiento);

		return aRespuesta(movimiento);
	}

	@Transactional(readOnly = true)
	public List<RespuestaMovimientoInventario> listar(UUID storeId, Instant from, Instant to, int limit) {
		int limiteSeguro = Math.max(1, Math.min(limit, 200));
		return repositorioMovimientos.list(storeId, from, to, PageRequest.of(0, limiteSeguro)).stream()
				.map(ServicioMovimientosInventario::aRespuesta).toList();
	}

	private static RespuestaMovimientoInventario aRespuesta(MovimientoInventario movimiento) {
		return new RespuestaMovimientoInventario(movimiento.getId(), movimiento.getMovementType(), movimiento.getProduct().getId(),
				movimiento.getProduct().getName(), movimiento.getProduct().getCode(), movimiento.getQuantity(),
				MapeadorUsuario.aRespuestaUsuario(movimiento.getUser()), movimiento.getObservation(), movimiento.getCreatedAt());
	}

	private static String normalizarOpcional(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isBlank() ? null : trimmed;
	}
}
