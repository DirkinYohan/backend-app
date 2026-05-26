package com.backend_app.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.backend_app.dto.ErrorApi;
import com.backend_app.util.ExcepcionConflicto;
import com.backend_app.util.ExcepcionNoAutorizado;
import com.backend_app.util.ExcepcionNoEncontrado;
import com.backend_app.util.ExcepcionProhibido;
import com.backend_app.util.ExcepcionSolicitudIncorrecta;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ManejadorExcepcionesGlobal {
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, Object> details = new LinkedHashMap<>();
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		details.put("fields", fieldErrors);

		return error(HttpStatus.BAD_REQUEST, "Validación inválida", request, details);
	}

	@ExceptionHandler(ExcepcionConflicto.class)
	public ResponseEntity<ErrorApi> manejarConflicto(ExcepcionConflicto ex, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(ExcepcionSolicitudIncorrecta.class)
	public ResponseEntity<ErrorApi> manejarSolicitudIncorrecta(ExcepcionSolicitudIncorrecta ex, HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(ExcepcionNoEncontrado.class)
	public ResponseEntity<ErrorApi> manejarNoEncontrado(ExcepcionNoEncontrado ex, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(ExcepcionNoAutorizado.class)
	public ResponseEntity<ErrorApi> manejarNoAutorizado(ExcepcionNoAutorizado ex, HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(ExcepcionProhibido.class)
	public ResponseEntity<ErrorApi> manejarProhibido(ExcepcionProhibido ex, HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorApi> manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "Conflicto de datos", request, Map.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorApi> manejarInesperado(Exception ex, HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", request, Map.of());
	}

	private static ResponseEntity<ErrorApi> error(HttpStatus status, String message, HttpServletRequest request,
			Map<String, Object> details) {
		ErrorApi apiError = new ErrorApi(Instant.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI(), details);
		return ResponseEntity.status(status).body(apiError);
	}
}
