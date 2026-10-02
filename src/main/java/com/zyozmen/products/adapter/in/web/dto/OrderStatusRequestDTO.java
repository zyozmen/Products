package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusRequestDTO(
        @NotBlank(message = "El estado es obligatorio") String status) { }
