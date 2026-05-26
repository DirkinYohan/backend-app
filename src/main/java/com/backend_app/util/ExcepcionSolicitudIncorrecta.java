package com.backend_app.util;

public class ExcepcionSolicitudIncorrecta extends RuntimeException {
	public ExcepcionSolicitudIncorrecta(String message) {
		super(message);
	}
}
