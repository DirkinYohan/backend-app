package com.backend_app.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class GeneradorRefreshToken {
	private static final SecureRandom RNG = new SecureRandom();

	private GeneradorRefreshToken() {
	}

	public static String nuevoToken() {
		byte[] bytes = new byte[48];
		RNG.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
