package com.backend_app.dto;

public record RespuestaAutenticacion(String accessToken, String refreshToken, RespuestaUsuario user) {
}
