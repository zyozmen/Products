package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCategory {
    private Long categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private Image image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
