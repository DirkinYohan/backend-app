package com.backend_app.util;

public class ExcepcionNoAutorizado extends RuntimeException {
	public ExcepcionNoAutorizado(String message) {
		super(message);
	}
}
