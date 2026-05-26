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

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, FiltroAutenticacionJwt filtroAutenticacionJwt,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
		entryPoint.setRealmName("api");

		AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> writeError(request,
				response, HttpServletResponse.SC_FORBIDDEN, "Prohibido", "Acceso denegado");

		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> writeError(
						request, response, HttpServletResponse.SC_UNAUTHORIZED, "No autorizado", "No autenticado"))
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class)
				.headers(h -> h.frameOptions(f -> f.sameOrigin()));

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

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

	private static void writeError(HttpServletRequest request, HttpServletResponse response, int status, String error,
			String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		String payload = "{\"timestamp\":\"" + java.time.Instant.now() + "\",\"status\":" + status + ",\"error\":\""
				+ escapeJson(error) + "\",\"message\":\"" + escapeJson(message) + "\",\"path\":\""
				+ escapeJson(request.getRequestURI()) + "\",\"details\":{}}";
		response.getWriter().write(payload);
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
