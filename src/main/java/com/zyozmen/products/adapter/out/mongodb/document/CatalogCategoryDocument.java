package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "Categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCategoryDocument {
    @Id
    private Long categoryId;
    @Indexed(unique = true, name = "category_name_unique")
    private String name;
    private String description;
    private String imageUrl;
    private ImageDocument image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
