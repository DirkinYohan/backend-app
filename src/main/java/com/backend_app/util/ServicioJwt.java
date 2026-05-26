package com.backend_app.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.backend_app.config.PropiedadesJwtSeguridad;
import com.backend_app.model.Rol;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class ServicioJwt {
	private final PropiedadesJwtSeguridad properties;
	private final SecretKey key;

	public ServicioJwt(PropiedadesJwtSeguridad properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String crearAccessToken(UUID userId, UUID storeId, Rol role, String email) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.accessTokenMinutes() * 60);
		Map<String, Object> claims = Map.of("uid", userId.toString(), "storeId", storeId.toString(), "role", role.name());

		return Jwts.builder()
				.subject(email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.claims(claims)
				.signWith(key)
				.compact();
	}

	public Claims parsearYValidar(String token) {
		return parseJws(token).getPayload();
	}

	private Jws<Claims> parseJws(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
		} catch (Exception e) {
			throw new ExcepcionNoAutorizado("Token inválido o expirado");
		}
	}
}
