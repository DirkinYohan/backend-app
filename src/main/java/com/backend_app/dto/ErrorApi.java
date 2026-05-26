package com.backend_app.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorApi(Instant timestamp, int status, String error, String message, String path, Map<String, Object> details) {
}
