package com.backend_app.dto;

import java.util.List;
import java.util.UUID;

import com.backend_app.model.MetodoPago;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SolicitudCrearVenta(@NotNull MetodoPago paymentMethod, @NotEmpty @Valid List<Item> items) {
	public record Item(@NotNull UUID productId, @Min(1) int quantity) {
	}
}
