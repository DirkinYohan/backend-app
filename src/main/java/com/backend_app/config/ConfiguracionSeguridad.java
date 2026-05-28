package com.backend_app.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Clase de configuración para la seguridad de la aplicación.
 * Define los filtros de seguridad, codificación de contraseñas y configuración de CORS.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {
	/**
	 * Configura la cadena de filtros de seguridad.
	 * 
	 * @param http Objeto HttpSecurity para configurar la seguridad web.
	 * @param filtroAutenticacionJwt Filtro personalizado para la autenticación mediante JWT.
	 * @param corsConfigurationSource Fuente de configuración de CORS.
	 * @return SecurityFilterChain configurada.
	 * @throws Exception Si ocurre un error durante la configuración.
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, FiltroAutenticacionJwt filtroAutenticacionJwt,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
		entryPoint.setRealmName("api");

		// Manejador para errores de acceso denegado (403 Forbidden)
		AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> writeError(request,
				response, HttpServletResponse.SC_FORBIDDEN, "Prohibido", "Acceso denegado");

		http.csrf(csrf -> csrf.disable()) // Deshabilita CSRF ya que usamos JWT y no sesiones
				.cors(cors -> cors.configurationSource(corsConfigurationSource)) // Configura CORS
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sin estado (Stateless)
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> writeError(
						request, response, HttpServletResponse.SC_UNAUTHORIZED, "No autorizado", "No autenticado"))
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**", "/h2-console/**").permitAll() // Rutas públicas
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permitir peticiones OPTIONS (CORS)
						.anyRequest().authenticated()) // El resto requiere autenticación
				.addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class) // Agregar filtro JWT
				.headers(h -> h.frameOptions(f -> f.sameOrigin())); // Permitir frames del mismo origen (para H2 Console)

		return http.build();
	}

	/**
	 * Define el codificador de contraseñas utilizando BCrypt.
	 * @return Instancia de BCryptPasswordEncoder.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Define el gestor de autenticación de Spring Security.
	 */
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	/**
	 * Configura el intercambio de recursos de origen cruzado (CORS).
	 * Permite peticiones desde el frontend (por defecto localhost:4200).
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(PropiedadesCors corsProperties) {
		List<String> allowedOrigins = Arrays.stream(corsProperties.allowedOrigins().split(",")).map(String::trim)
				.filter(s -> !s.isBlank()).toList();

		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins.isEmpty() ? List.of("http://localhost:4200") : allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	/**
	 * Escribe una respuesta de error en formato JSON.
	 */
	private static void writeError(HttpServletRequest request, HttpServletResponse response, int status, String error,
			String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		String payload = "{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":" + status + ",\"error\":\""
				+ escapeJson(error) + "\",\"message\":\"" + escapeJson(message) + "\",\"path\":\""
				+ escapeJson(request.getRequestURI()) + "\",\"details\":{}}";
		response.getWriter().write(payload);
	}

	/**
	 * Escapa caracteres especiales para el formato JSON.
	 */
	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
