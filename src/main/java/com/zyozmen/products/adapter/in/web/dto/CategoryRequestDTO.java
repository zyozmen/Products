package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @Size(max = 2048) @Pattern(regexp = "https?://[^\\s]+", message = "imageUrl debe ser una URL HTTP o HTTPS")
        String imageUrl) {
}
