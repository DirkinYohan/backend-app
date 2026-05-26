package com.backend_app.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FiltroMedicionRendimiento extends OncePerRequestFilter {
	private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, java.io.IOException {
		//#region debug-point render-db-latency-request-timing
		long startNs = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long ms = (System.nanoTime() - startNs) / 1_000_000L;
			String payload = "{\"sessionId\":\"" + escaparJson(sessionId()) + "\",\"hypothesisId\":\"H1|H2|H3|H4|H5\",\"runId\":\""
					+ escaparJson(runId()) + "\","
					+ "\"event\":\"http_timing\",\"data\":{"
					+ "\"method\":\"" + escaparJson(request.getMethod()) + "\","
					+ "\"path\":\"" + escaparJson(request.getRequestURI()) + "\","
					+ "\"status\":" + response.getStatus() + ","
					+ "\"durationMs\":" + ms
					+ "}}";
			reportar(payload);
		}
		//#endregion debug-point render-db-latency-request-timing
	}

	private static void reportar(String jsonPayload) {
		try {
			String url = debugServerUrl();
			if (url == null || url.isBlank()) return;
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.timeout(Duration.ofSeconds(2))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
					.build();
			CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
		} catch (Exception ignored) {
		}
	}

	private static String debugServerUrl() {
		String env = System.getenv("DEBUG_SERVER_URL");
		if (env != null && !env.isBlank()) return env;
		return leerEnvFile("DEBUG_SERVER_URL");
	}

	private static String sessionId() {
		String env = System.getenv("DEBUG_SESSION_ID");
		if (env != null && !env.isBlank()) return env;
		String fromFile = leerEnvFile("DEBUG_SESSION_ID");
		return (fromFile == null || fromFile.isBlank()) ? "render-db-latency" : fromFile;
	}

	private static String runId() {
		String env = System.getenv("DEBUG_RUN_ID");
		if (env != null && !env.isBlank()) return env;
		String fromFile = leerEnvFile("DEBUG_RUN_ID");
		return (fromFile == null || fromFile.isBlank()) ? "pre" : fromFile;
	}

	private static String leerEnvFile(String key) {
		Path p1 = Path.of(".dbg", "render-db-latency.env");
		Path p2 = Path.of("..", ".dbg", "render-db-latency.env");
		String v = leerEnvFileDesde(p1, key);
		if (v != null) return v;
		return leerEnvFileDesde(p2, key);
	}

	private static String leerEnvFileDesde(Path path, String key) {
		try {
			if (!Files.exists(path)) return null;
			for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
				int idx = line.indexOf('=');
				if (idx <= 0) continue;
				String k = line.substring(0, idx).trim();
				if (!k.equals(key)) continue;
				return line.substring(idx + 1).trim();
			}
			return null;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String escaparJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}
}
